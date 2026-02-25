package android.tester.backend.services.storage;

import android.tester.backend.dtos.ApplicationJobScreenshotsResponse;
import android.tester.backend.entities.ScreenShot;
import android.tester.backend.repositories.ScreenshotRepository;
import android.tester.backend.services.logging.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorageService {

  private static final String BASE_DIR = "C:\\Users\\kaspa\\Documents\\Coding\\Bachellor-project-2025\\database_files";
  private final ScreenshotRepository screenshotRepository;
  private final LogService logger;

  public String saveApk(MultipartFile file, UUID userId) throws IOException {
    if (file.isEmpty()) {
      throw new IOException("Failed to store empty file.");
    }

    // Structure: BASE_DIR / userId / apks / filename
    String userPath = Paths.get(BASE_DIR, userId.toString(), "apks").toString();
    createDirectory(userPath);

    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
    Path destination = Paths.get(userPath, fileName);

    Files.copy(file.getInputStream(), destination);

    return destination.toAbsolutePath().toString();
  }

  public String createTestRunOutputDirectory(UUID userId, String packageName, UUID testRunId) {
    // Structure: BASE_DIR / userId / packageName / testRun / files
    Path path = Paths.get(BASE_DIR, userId.toString(), packageName, testRunId.toString(), "files");
    createDirectory(path.toString());
    return path.toAbsolutePath().toString();
  }

  // NEW METHOD: Saves generated defect proof images
  public String saveDefectImage(BufferedImage image, UUID userId, String packageName, UUID testRunId) throws IOException {
    // Structure: BASE_DIR / userId / packageName / testRun / defect_images
    Path dirPath = Paths.get(BASE_DIR, userId.toString(), packageName, testRunId.toString(), "defect_images");
    createDirectory(dirPath.toString());

    String fileName = "defect_" + UUID.randomUUID() + ".png";
    Path filePath = dirPath.resolve(fileName);

    ImageIO.write(image, "png", filePath.toFile());

    return fileName; // Return just the filename or filePath.toString() depending on retrieval preference
  }

  private void createDirectory(String path) {
    File directory = new File(path);
    if (!directory.exists()) {
      directory.mkdirs();
    }
  }

  public ApplicationJobScreenshotsResponse getApplicationScreenshotsGroupedByJob(UUID applicationId) {
    logger.addLog("Getting screenshots for application: " + applicationId);
    List<ScreenShot> screenshots = screenshotRepository.findByApplicationId(applicationId);
    logger.addLog("Found " + screenshots.size() + " screenshots for application: " + applicationId);
    if (screenshots.isEmpty()) {
      return null;
    }

    String appName = screenshots.get(0).getApplication().getName();
    logger.addLog("Application name: " + appName);

    var jobsData = screenshots.stream()
      .filter(s -> s.getTestDevice() != null && s.getTestDevice().getTestRun() != null)
      .collect(Collectors.groupingBy(s -> s.getTestDevice().getTestRun()))
      .entrySet().stream()
      .map(entry -> {
        var testRun = entry.getKey();
        var job = testRun.getJob();
        var meta = entry.getValue().stream()
          .map(s -> new ApplicationJobScreenshotsResponse.ScreenshotMetadataDTO(
            s.getId(),
            s.getFileName(),
            String.format("/api/v1/images/%s/%s/%s/%s",
              testRun.getUser().getId(),
              s.getApplication().getPackageName(),
              testRun.getId(),
              s.getFileName())
          )).toList();

        return new ApplicationJobScreenshotsResponse.JobGroupDTO(
          job != null ? job.getId() : null,
          testRun.getId(),
          meta
        );
      }).toList();

    return new ApplicationJobScreenshotsResponse(applicationId, appName, jobsData);
  }
}
