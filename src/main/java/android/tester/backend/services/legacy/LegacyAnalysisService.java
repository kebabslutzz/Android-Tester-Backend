package android.tester.backend.services.legacy;

import android.tester.backend.entities.*;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.IStateRuleChecker;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.StateCheckResults;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.AppContext;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;
import android.tester.backend.repositories.DefectRepository;
import android.tester.backend.repositories.DefectTypeRepository;
import android.tester.backend.repositories.TestRunDefectImageRepository;
import android.tester.backend.repositories.TestRunDefectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LegacyAnalysisService {

  private final List<IStateRuleChecker> checkers;
  private final DefectTypeRepository defectTypeRepository;
  private final DefectRepository defectRepository;
  private final TestRunDefectRepository testRunDefectRepository;
  private final TestRunDefectImageRepository testRunDefectImageRepository;

  /**
   * Analyzes a single screenshot using the legacy logic.
   *
   * @param screenShot The database entity representing the screenshot.
   * @param imageFile  The physical PNG file on disk (from DroidBot).
   * @param layoutFile The physical JSON/XML hierarchy file on disk (from DroidBot).
   */
  @Transactional
  public void analyzeScreenshot(ScreenShot screenShot, File imageFile, File layoutFile,
                                android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application app,
                                File dataFolder, List<android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.TestDevice> testDevices,
                                android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.SystemContext systemContext) {
    if (!imageFile.exists() || !layoutFile.exists()) {
      System.err.println("Files missing for analysis: " + screenShot.getId());
      return;
    }

    try {
      // 1. Construct the Legacy 'context' objects
      AppContext legacyContext = new AppContext(app, dataFolder, testDevices, systemContext);

      // Construct the State object required by legacy checkers
      // Assuming constructor: State(String name, AppContext context, File image, File config, File logic)
      State legacyState = new State(
        screenShot.getFileName(),
        legacyContext,
        imageFile,
        layoutFile,
        null // logic file nullable?
      );

      // 2. Prepare Results Collector
      StateCheckResults legacyResults = new StateCheckResults(legacyState);

      // 3. Execute all registered checkers
      for (IStateRuleChecker checker : checkers) {
        try {
          checker.analyze(legacyState, legacyResults);
        } catch (Exception e) {
          System.err.println("Legacy checker failed: " + checker.getClass().getSimpleName());
          e.printStackTrace();
        }
      }

      // 4. Map Legacy Results -> Database Entities
      for (var annotation : legacyResults.getAnnotations()) {
        String ruleCode = annotation.defect().getRuleCode();
        String description = annotation.message(); // or checker name?

        // Find or Create DefectType
        DefectType defectType = defectTypeRepository.findByCode(ruleCode)
          .orElseGet(() -> defectTypeRepository.save(
            DefectType.builder()
              .code(ruleCode)
              .description(description)
              .created(OffsetDateTime.now())
              .build()
          ));

        // Save Defect (The low-level finding)
        Defect defect = Defect.builder()
          .screenShot(screenShot)
          .defectType(defectType)
          .created(OffsetDateTime.now())
          .build();

        defectRepository.save(defect);

        // Save TestRunDefect (The report item)
        // We aggregate specific defects into a report item.
        // For simplicity, we create one TestRunDefect per Defect found here,
        // but typically you might group them by DefectType + TestRun.
        // Keeping it simple as per "add the said defected image..." request.

        TestRun testRun = screenShot.getTestDevice().getTestRun();
        Application application = screenShot.getApplication();

        TestRunDefect testRunDefect = TestRunDefect.builder()
          .testRun(testRun)
          .application(application)
          .screenShot(screenShot)
          .defectType(defectType)
          .defectsCount(1L)
          .message(description)
          .build();

        testRunDefect = testRunDefectRepository.save(testRunDefect);

        // Create Visual Proof Image
        // Draw the bounding box on the image
        if (annotation.bounds() != null) {
          try {
            BufferedImage originalImage = ImageIO.read(imageFile);
            Graphics2D g2d = originalImage.createGraphics();

            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(annotation.bounds().x, annotation.bounds().y, annotation.bounds().width, annotation.bounds().height);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(originalImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            TestRunDefectImage defectImage = TestRunDefectImage.builder()
              .testRunDefect(testRunDefect)
              .imageData(imageBytes)
              .created(OffsetDateTime.now())
              .build();

            testRunDefectImageRepository.save(defectImage);

          } catch (Exception e) {
            System.err.println("Failed to create visual proof for defect: " + ruleCode);
            e.printStackTrace();
          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
