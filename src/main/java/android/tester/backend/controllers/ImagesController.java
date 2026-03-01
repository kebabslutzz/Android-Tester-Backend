package android.tester.backend.controllers;

import android.tester.backend.dtos.screenshots.ApplicationJobScreenshotsResponseRecord;
import android.tester.backend.services.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImagesController {

  private final String BASE_DIR = "C:/Users/kaspa/Documents/Coding/Bachellor-project-2025/database_files/";
  private final StorageService storageService;

  @GetMapping("/application/{applicationId}")
  public ApplicationJobScreenshotsResponseRecord getApplicationScreenshots(@PathVariable UUID applicationId, Principal connectedUser) {
    return storageService.getApplicationScreenshotsGroupedByJob(applicationId, connectedUser);
  }

  @GetMapping("/{userId}/{packageName}/{testRunId}/{filename}")
  public ResponseEntity<Resource> serveScreenshot(
    @PathVariable String packageName,
    @PathVariable String testRunId,
    @PathVariable String filename,
    Principal connectedUser
  ) throws Exception {
    return storageService.serveScreenshot(packageName, testRunId, filename, connectedUser);
  }
}
