package android.tester.backend.services.legacy;

import android.tester.backend.entities.ScreenShot;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.IStateRuleChecker;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.checks.StateCheckResults;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.AppContext;
import android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.context.State;
import android.tester.backend.repositories.DefectRepository;
import android.tester.backend.repositories.DefectTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LegacyAnalysisService {

  private final List<IStateRuleChecker> checkers;
  private final DefectTypeRepository defectTypeRepository;
  private final DefectRepository defectRepository;

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
      // Assuming legacyResults.getAnnotations() returns the list of defects found
      // You will need to inspect the actual 'StateCheckResults' class to see how to get data out.

             /*
             // Pseudo-code implementation based on typical legacy structure:
             for (var annotation : legacyResults.getAnnotations()) {
                 String limitMsg = annotation.getMessage();
                 String ruleCode = annotation.getCheck().getRuleCode(); // e.g. "TS2"

                 // Find or Create DefectType
                 DefectType defectType = defectTypeRepository.findByCode(ruleCode)
                     .orElseGet(() -> defectTypeRepository.save(
                         DefectType.builder()
                             .code(ruleCode)
                             .description(annotation.getCheck().getName())
                             .created(new Date())
                             .build()
                     ));

                 // Save Defect
                 Defect defect = Defect.builder()
                     .screenShot(screenShot)
                     .defectType(defectType)
                     .created(new Date())
                     // You could store the bounding box (Rect) in a new column or description
                     .build();

                 defectRepository.save(defect);
             }
             */

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
