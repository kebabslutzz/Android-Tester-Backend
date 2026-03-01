package android.tester.backend.services.storage;

import android.tester.backend.dtos.screenshots.ApplicationJobScreenshotsResponseRecord;
import android.tester.backend.entities.Application;
import android.tester.backend.entities.ScreenShot;
import android.tester.backend.entities.User;
import android.tester.backend.exceptions.NotFoundException;
import android.tester.backend.exceptions.ValidationException;
import android.tester.backend.repositories.ApplicationRepository;
import android.tester.backend.repositories.ScreenshotRepository;
import android.tester.backend.services.logging.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorageService {

  private static final String BASE_DIR = "C:\\Users\\kaspa\\Documents\\Coding\\Bachellor-project-2025\\database_files";
  private final ScreenshotRepository screenshotRepository;
  private final ApplicationRepository applicationRepository;
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

  public Application saveApkAndCreateApplication(MultipartFile file, Principal connectedUser) throws IOException {
    var currentUser = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

    if (file.isEmpty()) {
      throw new ValidationException("Cannot upload an empty file.");
    }

    String userPath = Paths.get(BASE_DIR, currentUser.getId().toString(), "apks").toString();
    createDirectory(userPath);

    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
    Path destination = Paths.get(userPath, fileName);
    Files.copy(file.getInputStream(), destination);

    String packageName = "unknown.package";
    String version = "1.0";
    String appName = file.getOriginalFilename();

    try (net.dongliu.apk.parser.ApkFile apkFile = new net.dongliu.apk.parser.ApkFile(destination.toFile())) {
      net.dongliu.apk.parser.bean.ApkMeta meta = apkFile.getApkMeta();
      packageName = meta.getPackageName();
      version = meta.getVersionName();
      appName = meta.getLabel();
    } catch (Exception e) {
      logger.addLog("Could not parse APK metadata: " + e.getMessage());
    }

    Application application = Application.builder()
      .name(appName)
      .packageName(packageName)
      .version(version)
      .apkFile(fileName)
      .apkPath(destination.toAbsolutePath().toString())
      .versionNumber(0)
      .created(OffsetDateTime.now())
      .build();

    return applicationRepository.save(application);
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

  public ApplicationJobScreenshotsResponseRecord getApplicationScreenshotsGroupedByJob(UUID applicationId, Principal connectedUser) {
    var currentUser = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

    List<ScreenShot> screenshots = screenshotRepository.findByApplicationId(applicationId);
    if (screenshots.isEmpty()) {
      return null;
    }

    List<ScreenShot> userScreenshots = screenshots.stream()
      .filter(s -> s.getTestDevice() != null
        && s.getTestDevice().getTestRun() != null
        && s.getTestDevice().getTestRun().getUser().getId().equals(currentUser.getId()))
      .toList();

    if (userScreenshots.isEmpty()) {
      return null;
    }

    String appName = screenshots.get(0).getApplication().getName();

    var jobsData = screenshots.stream()
      .filter(s -> s.getTestDevice() != null && s.getTestDevice().getTestRun() != null)
      .collect(Collectors.groupingBy(s -> s.getTestDevice().getTestRun()))
      .entrySet().stream()
      .map(entry -> {
        var testRun = entry.getKey();
        var job = testRun.getJob();
        var meta = entry.getValue().stream()
          .map(s -> new ApplicationJobScreenshotsResponseRecord.ScreenshotMetadataDTO(
            s.getId(),
            s.getFileName(),
            String.format("/api/v1/images/%s/%s/%s/%s",
              testRun.getUser().getId(),
              s.getApplication().getPackageName(),
              testRun.getId(),
              s.getFileName())
          )).toList();

        return new ApplicationJobScreenshotsResponseRecord.JobGroupDTO(
          job != null ? job.getId() : null,
          testRun.getId(),
          meta
        );
      }).toList();

    return new ApplicationJobScreenshotsResponseRecord(applicationId, appName, jobsData);
  }

  public ResponseEntity<Resource> serveScreenshot(String packageName, String testRunId, String filename, Principal connectedUser) throws Exception {
    var currentUser = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

    Path filePath = Paths.get(BASE_DIR, currentUser.getId().toString(), packageName, testRunId, "files", "states", filename);
    Resource resource = new UrlResource(filePath.toUri());

    if (!resource.exists() || !resource.isReadable()) {
      throw new NotFoundException("Screenshot not found: " + filename);
    }
    return ResponseEntity.ok()
      .contentType(MediaType.IMAGE_PNG)
      .body(resource);
  }
}
