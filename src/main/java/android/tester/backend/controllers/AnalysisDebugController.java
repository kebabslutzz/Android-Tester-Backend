package android.tester.backend.controllers;

import android.tester.backend.entities.ScreenShot;
import android.tester.backend.repositories.ApplicationRepository;
import android.tester.backend.repositories.JobRepository;
import android.tester.backend.repositories.ScreenshotRepository;
import android.tester.backend.repositories.TestDeviceRepository;
import android.tester.backend.services.legacy.LegacyAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/debug/analysis")
@RequiredArgsConstructor
public class AnalysisDebugController {

  private final ScreenshotRepository screenShotRepository;
  private final JobRepository jobRepository;
  private final TestDeviceRepository testDeviceRepository;
  private final ApplicationRepository applicationRepository;
  private final LegacyAnalysisService legacyAnalysisService; // Make sure this service exists or is available

  @PostMapping("/rerun")
  public ResponseEntity<String> rerunAnalysis() {
    // Hardcoded IDs from user request
    UUID jobId = UUID.fromString("8f59b351-df6b-49ab-beae-229aa7ce6228");
    UUID deviceId = UUID.fromString("4ba5224b-2a0d-4578-a0b2-663333b2d5ff");
    UUID appId = UUID.fromString("7b4fe15d-b7c7-4bb2-ab00-e625526d7a00");

    // Path to files
    String filesPath = "C:\\Users\\kaspa\\Documents\\Coding\\Bachellor-project-2025\\database_files\\00000000-0000-0000-0000-000000000001\\unknown.package\\4a22ed23-609c-4b3f-8430-f324cd2841ce\\files";
    File statesFolder = new File(filesPath, "states");

    if (!statesFolder.exists() || !statesFolder.isDirectory()) {
      return ResponseEntity.badRequest().body("States folder not found at: " + statesFolder.getAbsolutePath());
    }

    var job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
    var app = applicationRepository.findById(appId).orElseThrow(() -> new RuntimeException("App not found"));
    var device = testDeviceRepository.findById(deviceId).orElseThrow(() -> new RuntimeException("Device not found"));

    // Prepare Legacy App Context
    android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application legacyApp =
      new android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application(
        0,
        ".",
        true,
        "", // APK path might be irrelevant for analysis based on existing JSONs
        app.getPackageName(),
        app.getPackageName()
      );

    File[] files = statesFolder.listFiles();
    int processedCount = 0;

    if (files != null) {
      for (File file : files) {
        String fileName = file.getName();

        if (fileName.endsWith(".png") && fileName.startsWith("screen_")) {
          // Identify corresponding JSON
          String jsonFileName = fileName.replace("screen_", "state_").replace(".png", ".json");
          File layoutFile = new File(statesFolder, jsonFileName);

          if (layoutFile.exists()) {

            // Find existing ScreenShot or create simplified reference
            // Since user said "all of them are already saved in database", we search for them
            // We search by unique constraint if exists, or just use the first match for this device/app/file
            // Assuming ScreenShot entity stores the filename correctly.

            // Depending on how ScreenShots are stored, finding the exact DB record might be tricky
            // if we filter by just filename.
            // We'll iterate the known screenshots for this device/app combo or just try to find by filename.

            ScreenShot screenShot = null;

            // Try to find one in DB
            // Note: You might need a method findByFileNameAndTestDevice in your repo
            // For now, let's look for any screenshot with this filename
            var potentialshots = screenShotRepository.findAll().stream()
              .filter(s -> s.getFileName().equals(fileName))
              .filter(s -> s.getTestDevice().getId().equals(deviceId))
              .findFirst();

            if (potentialshots.isPresent()) {
              screenShot = potentialshots.get();
              log.info("Re-analyzing screenshot: " + screenShot.getId());

              try {
                legacyAnalysisService.analyzeScreenshot(
                  screenShot,
                  file,
                  layoutFile,
                  legacyApp,
                  statesFolder,
                  Collections.emptyList(),
                  null
                );
                processedCount++;
              } catch (Exception e) {
                log.error("Failed to analyze " + fileName, e);
              }
            } else {
              log.warn("Screenshot entity not found in DB for file: " + fileName);
            }
          }
        }
      }
    }

    return ResponseEntity.ok("Analysis triggered. Processed " + processedCount + " images.");
  }
}
