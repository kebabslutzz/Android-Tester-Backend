package android.tester.backend.controllers;

import android.tester.backend.dtos.ApplicationJobScreenshotsResponse;
import android.tester.backend.services.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImagesController {

  private final String BASE_DIR = "C:/Users/kaspa/Documents/Coding/Bachellor-project-2025/database_files/";
  private final StorageService storageService;

  @GetMapping("/application/{applicationId}")
  public ResponseEntity<ApplicationJobScreenshotsResponse> getApplicationScreenshots(@PathVariable UUID applicationId) {
    ApplicationJobScreenshotsResponse response = storageService.getApplicationScreenshotsGroupedByJob(applicationId);
    return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
  }

  @GetMapping("/{userId}/{packageName}/{testRunId}/{filename}")
  public ResponseEntity<org.springframework.core.io.Resource> serveScreenshot(
    @PathVariable String userId,
    @PathVariable String packageName,
    @PathVariable String testRunId,
    @PathVariable String filename) throws Exception {

    Path filePath = Paths.get(BASE_DIR, userId, packageName, testRunId, "files", "states", filename);
    Resource resource = new UrlResource(filePath.toUri());

    if (resource.exists() || resource.isReadable()) {
      return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(resource);
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
