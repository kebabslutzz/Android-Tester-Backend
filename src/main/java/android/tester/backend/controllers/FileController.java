package android.tester.backend.controllers;

import android.tester.backend.dtos.application.ApplicationInfoDTO;
import android.tester.backend.dtos.screenshots.JobScreenshotDTO;
import android.tester.backend.services.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

  private final String BASE_DIR = "C:/Users/kaspa/Documents/Coding/Bachellor-project-2025/database_files/";
  private final StorageService storageService;

  @GetMapping("/applications")
  public List<ApplicationInfoDTO> getUserApplications(Principal connectedUser) {
    return storageService.getUserApplicationsWithJobs(connectedUser);
  }

  @GetMapping("/application-screenshots")
  public JobScreenshotDTO getApplicationScreenshots(@RequestParam UUID applicationId, Principal connectedUser) {
    return storageService.getApplicationScreenshotsGroupedByJob(applicationId, connectedUser);
  }

  @GetMapping("/{packageName}/{testRunId}/{filename}")
  public ResponseEntity<Resource> serveScreenshot(
    @PathVariable String packageName,
    @PathVariable String testRunId,
    @PathVariable String filename,
    Principal connectedUser
  ) throws Exception {
    return storageService.serveScreenshot(packageName, testRunId, filename, connectedUser);
  }

  @GetMapping("/defects/{packageName}/{testRunId}/{filename}")
  public ResponseEntity<Resource> serveDefectImage(
    @PathVariable String packageName,
    @PathVariable String testRunId,
    @PathVariable String filename,
    Principal connectedUser
  ) throws Exception {
    return storageService.serveDefectImage(packageName, testRunId, filename, connectedUser);
  }

}
