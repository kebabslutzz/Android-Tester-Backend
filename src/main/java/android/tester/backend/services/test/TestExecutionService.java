package android.tester.backend.services.test;

import android.tester.backend.entities.*;
import android.tester.backend.repositories.JobRepository;
import android.tester.backend.repositories.JobStatusRepository;
import android.tester.backend.repositories.ScreenshotRepository;
import android.tester.backend.services.adb.AdbConnectionService;
import android.tester.backend.services.legacy.LegacyAnalysisService;
import android.tester.backend.services.logging.LogService;
import android.tester.backend.services.shellCommands.ShellCommandService;
import android.tester.backend.services.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TestExecutionService {

  private final JobRepository jobRepository;
  private final JobStatusRepository jobStatusRepository;
  private final ShellCommandService shellCommandService;
  private final StorageService storageService;
  private final LegacyAnalysisService legacyAnalysisService;
  private final ScreenshotRepository screenshotRepository;
  private final LogService logService;
  private final AdbConnectionService adbConnectionService;
  private static final String LOCAL_AVD_NAME = "TestDevice";

  @Async
  public void executeJob(UUID jobId) {
    Optional<Job> jobOpt = jobRepository.findById(jobId);
    if (jobOpt.isEmpty()) {
      logService.addLog("Error: Job not found for ID: " + jobId);
      return;
    }
    Job job = jobOpt.get();

    logService.addLog("Started test execution for job: " + job.getId());

    try {
      // 1. Ensure Local Emulator is running
      // We no longer connect to Docker. We check local adb devices.
      if (!isLocalDeviceOnline()) {
        // Restart ADB for a clean state
        shellCommandService.runCommandSync(List.of("adb", "kill-server"));
        shellCommandService.runCommandSync(List.of("adb", "start-server"));
        logService.addLog("ADB restarted.");

        logService.addLog("Local device not found. Attempting to launch AVD: " + LOCAL_AVD_NAME);
        startLocalEmulator();
        waitForDeviceOnline();
        Thread.sleep(3000);
        installDroidbotApp();
      } else {
        logService.addLog("Local device already online.");
      }

      TestRun testRun = job.getTestRun();
      Application app = job.getApplication();

      String actualApkPath = app.getApkFile();
      if (actualApkPath == null || actualApkPath.isEmpty()) {
        throw new RuntimeException("APK file path not found");
      }

      String outputDir = storageService.createTestRunOutputDirectory(
        testRun.getUser().getId(),
        app.getPackageName(),
        testRun.getId()
      );

      String deviceSerial = getDeviceSerial();
      if (deviceSerial == null) {
        throw new RuntimeException("No online device found");
      }

      Thread.sleep(3000);

      // 2. Build Droidbot command for LOCAL execution
      List<String> command = new ArrayList<>();
      command.add("py");
      command.add("-3.7-32");
      command.add("-m");
      command.add("droidbot.start");

      // TARGETING: When running locally, we usually don't need -d localhost:5555
      // unless multiple devices are connected. Droidbot picks the first connected
      // one by default. If you want to be specific, use the serial (usually emulator-5554).
      command.add("-d");
      command.add(deviceSerial);

      command.add("-a");
      command.add(actualApkPath);
      command.add("-o");
      command.add(outputDir);

      command.add("-is_emulator");
      command.add("-timeout");
      command.add("90");
      command.add("-keep_app");


      logService.addLog("Starting Droidbot Locally on " + deviceSerial + ": " + command);

      // 3. Run Command
      String result = shellCommandService.runCommandSync(command);

      logService.addLog("Droidbot finished.");

      // 3. Analyzing screenshots
      updateJobStatus(job, "Analyzing screenshots");

      File droidbotOutputFolder = new File(outputDir);
      File statesFolder = new File(droidbotOutputFolder, "states");

      logService.addLog("Checking for screenshots in: " + statesFolder.getAbsolutePath());

      if (statesFolder.exists() && statesFolder.isDirectory()) {
        File[] files = statesFolder.listFiles();

        if (files != null) {
          logService.addLog("Found " + files.length + " files in states directory.");

          int processedCount = 0;
          for (File file : files) {
            String fileName = file.getName();
            // Process PNG files
            if (fileName.endsWith(".png") && fileName.startsWith("screen_")) {

              // Map: screen_2023...png -> state_2023...json
              String jsonFileName = fileName.replace("screen_", "state_").replace(".png", ".json");
              File layoutFile = new File(statesFolder, jsonFileName);

              // Fallback: If exact timestamp match fails, looks for ANY json that is close or simply the only one?
              // For now, let's log if missed
              if (!layoutFile.exists()) {
                logService.addLog("WARNING: Could not find layout file: " + jsonFileName + " for image " + fileName);
                // Optional: iterate files to find closest match if timestamps drift slightly?
                // But usually Droidbot tags are consistent per event.
              }

              if (layoutFile.exists()) {
                ScreenShot screenShot = ScreenShot.builder()
                  .fileName(fileName)
                  .testDevice(testRun.getTestDevice())
                  .application(job.getApplication())
                  .created(OffsetDateTime.now())
                  .build();

                screenShot = screenshotRepository.save(screenShot);
                processedCount++;

                // Prepare context for legacy analysis
                android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application legacyApp =
                  new android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application(
                    0,
                    ".",
                    true,
                    actualApkPath,
                    app.getPackageName(),
                    app.getPackageName()
                  );

                legacyAnalysisService.analyzeScreenshot(
                  screenShot,
                  file,
                  layoutFile,
                  legacyApp,
                  statesFolder, // data folder
                  List.of(),
                  null
                );
              }
            }
          }
          logService.addLog("Analyzed " + processedCount + " screenshots.");
        }
      } else {
        logService.addLog("WARNING: 'states' folder not found. Droidbot might have failed to traverse.");
      }

      // 4. Done
      updateJobStatus(job, "Done");

//      // 4. Update Status
//      JobStatus completedStatus = jobStatusRepository.findByName("Completed");
//      if (completedStatus == null) {
//        completedStatus = JobStatus.builder().name("Completed").build();
//        jobStatusRepository.save(completedStatus);
//      }
//      job.setStatus(completedStatus);
//      jobRepository.save(job);

    } catch (Exception e) {
      logService.addLog("Job Failed: " + e.getMessage());
      e.printStackTrace();
    } finally {
      // CLEANUP SECTION
      performCleanup(job.getApplication().getPackageName());
    }
  }

//  @Async
//  public void executeJob(UUID jobId) {
//    Optional<Job> jobOpt = jobRepository.findById(jobId);
//    if (jobOpt.isEmpty()) {
//      logService.addLog("Error: Job not found for ID: " + jobId);
//      return;
//    }
//    Job job = jobOpt.get();
//
//    logService.addLog("Started test execution for job: " + job.getId());
//
//    try {
//      // 1. ADV Started
//      updateJobStatus(job, "ADV Started");
//      // For now, we assume the emulator is managed externally or we are connecting to a static one.
//      // In a full implementation, we would docker run here.
//      adbConnectionService.connectToContainer();
//
//      logService.addLog("ADB connected");
//
//      TestRun testRun = job.getTestRun();
//      String apkPath = Paths.get(
//        "database_files",
//        testRun.getUser().getId().toString(),
//        "apks"
//      ).toAbsolutePath().toString(); // Use logic from StorageService to find actual file, simplified here
//
//      java.io.File apkDir = new java.io.File(apkPath);
//      java.io.File[] files = apkDir.listFiles((dir, name) -> name.endsWith(".apk"));
//
//      // 2. Droidbot running
//      updateJobStatus(job, "Droidbot running");
//
//      String packageName = job.getApplication().getPackageName(); // Assuming we have package name
//      String apkPath = job.getApplication().getApkFile();
//      String outputDir = storageService.createTestRunOutputDirectory(
//        testRun.getUser().getId(),
//        packageName != null ? packageName : "unknown_app",
//        testRun.getId()
//      );
//
//      // Example command - adjust Droidbot args as needed
//      // droidbot -a <apk> -o <output> -is_emulator -keep_env -policy dfs -count 50
//      List<String> droidbotCommand = List.of(
////        "droidbot",
//        "py", "-3.7-32", "-m", "droidbot.start",
//        "-a", apkPath,
//        "-o", outputDir,
//        "-is_emulator",
//        "-timeout", "60"

  /// /        "-policy", "dfs", // depth-first search
  /// /        "-count", "10"    // limit for testing
//      );
//
//      logService.addLog("Starting Droidbot: " + droidbotCommand);
//      String result = shellCommandService.runCommandSync(droidbotCommand);
//      logService.addLog("Droidbot finished: " + result);
//
//      // 3. Analyzing screenshots
//      updateJobStatus(job, "Analyzing screenshots");
//
//      File outputFolder = new File(outputDir);
//      File[] files = outputFolder.listFiles();
//
//      if (files != null) {
//        for (File file : files) {
//          if (file.getName().endsWith(".png")) {
//            // Droidbot saves structure: screen_2023...png
//            // and corresponding json or xml maybe?
//            // Legacy service expects: Screenshot entity, imageFile, layoutFile
//
//            // Look for layout file (json)
//            String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
//            File layoutFile = new File(outputFolder, baseName + ".json");
//            if (!layoutFile.exists()) {
//              layoutFile = new File(outputFolder, baseName + ".xml"); // fallback
//            }
//
//            if (layoutFile.exists()) {
//              ScreenShot screenShot = ScreenShot.builder()
//                .fileName(file.getName())
//                .testDevice(testRun.getTestDevice()) // assuming link
//                .application(job.getApplication())
//                .created(OffsetDateTime.now())
//                .build();
//
//              screenShot = screenshotRepository.save(screenShot);
//
//              // Call Legacy Analysis
//              // We need to pass required Legacy Context objects.
//              // For MVP, passing nulls where acceptable or creating dummy objects
//              // Application wrapper for legacy
//              android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application legacyApp =
//                new android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application(
//                  0,
//                  ".",
//                  true,
//                  apkPath,
//                  packageName,
//                  packageName
//                );
//
//              legacyAnalysisService.analyzeScreenshot(
//                screenShot,
//                file,
//                layoutFile,
//                legacyApp, // mapped app
//                outputFolder, // data folder
//                List.of(), // testDevices list (can be empty if not strictly used by single screen check)
//                null // systemContext
//              );
//            }
//          }
//        }
//      }
//
//      // 4. Done
//      updateJobStatus(job, "Done");
//
//    } catch (Exception e) {
//      e.printStackTrace();
//      logService.addLog("Job failed: " + e.getMessage());
//      updateJobStatus(job, "Error");
//    }
//  }
  private String getDeviceSerial() {
    try {
      List<String> command = new ArrayList<>();
      command.add("adb");
      command.add("devices");
      String result = shellCommandService.runCommandSync(command);
      // Parse output for the first device in "device" state
      String[] lines = result.split("\n");
      for (String line : lines) {
        if (line.contains("device") && !line.contains("List of devices attached")) {
          String[] parts = line.trim().split("\\s+");
          if (parts.length >= 2 && "device".equals(parts[1])) {
            return parts[0];
          }
        }
      }
    } catch (Exception e) {
      logService.addLog("Error detecting device serial: " + e.getMessage());
    }
    return null;
  }

//  private boolean isLocalDeviceOnline() {
//    try {
//      List<String> command = new ArrayList<>();
//      command.add("adb");
//      command.add("devices");
//      String result = shellCommandService.runCommandSync(command);

  /// /      ProcessBuilder pb = new ProcessBuilder("adb", "devices");
  /// /      Process process = pb.start();
  /// /      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
  /// /        String line;
  /// /        while ((line = reader.readLine()) != null) {
  /// /          // Local emulators usually show up as emulator-5554
  /// /          if (line.contains("emulator") && line.contains("device")) {
  /// /            return true;
  /// /          }
  /// /        }
  /// /      }
  /// /      process.waitFor();
  /// /      return result.contains("emulator-5554") || result.contains("emulator-5556");
//      return (result.contains("emulator-5554") && result.contains("device")) ||
//        (result.contains("emulator-5556") && result.contains("device")) ||
//        (result.contains("localhost:5555") && result.contains("device"));
//    } catch (Exception e) {
//      logService.addLog("Error checking local devices: " + e.getMessage());
//    }
//    return false;
//  }
  private boolean isLocalDeviceOnline() {
    try {
      List<String> command = new ArrayList<>();
      command.add("adb");
      command.add("devices");
      String result = shellCommandService.runCommandSync(command);
      String[] lines = result.split("\n");
      for (String line : lines) {
        if (line.contains("device") && !line.contains("List of devices attached")) {
          return true;
        }
      }
    } catch (Exception e) {
      logService.addLog("Error checking local devices: " + e.getMessage());
    }
    return false;
  }

  private void startLocalEmulator() {
    // This assumes 'emulator' is in your Windows Path.
    // If not, use full path: C:\Users\kaspa\AppData\Local\Android\Sdk\emulator\emulator.exe
    List<String> cmd = List.of(
      "emulator",
      "-avd", LOCAL_AVD_NAME,
      "-no-snapshot-load",
      "-no-boot-anim",
      "-netdelay", "none",
      "-netspeed", "full"
    );

    try {
//      ProcessBuilder pb = new ProcessBuilder(cmd);
      // Important: start emulator as a detached process or it will block the Java thread
//      pb.start();
      shellCommandService.runCommandAsync(cmd, null);
      logService.addLog("Emulator launch command issued. Waiting for boot...");
      // Give it a few seconds to register with ADB before we start polling
      Thread.sleep(10000);
    } catch (Exception e) {
      throw new RuntimeException("Failed to start emulator: " + e.getMessage());
    }
  }

  private void waitForDeviceOnline() {
    int maxRetries = 90; // Wait up to ~4.5 minutes for a cold boot
    int retryCount = 0;

    logService.addLog("Waiting for local emulator to come online...");

    while (retryCount < maxRetries) {
      if (isLocalDeviceOnline()) {
        try {
//          List<String> command = new ArrayList<>();
//          command.add("adb");
//          command.add("-s");
//          command.add("emulator-5554");
//          command.add("shell");
//          command.add("getprop");
//          command.add("sys.boot_completed");

          // Check if boot is actually complete (UI is ready)
          ProcessBuilder bootCheck = new ProcessBuilder("adb", "-s", "emulator-5554", "shell", "getprop", "sys.boot_completed");
          Process bootProcess = bootCheck.start();
          try (BufferedReader bootReader = new BufferedReader(new InputStreamReader(bootProcess.getInputStream()))) {
            String bootLine = bootReader.readLine();
            if ("1".equals(bootLine != null ? bootLine.trim() : "0")) {
              logService.addLog("Device is online and fully booted!");
              return;
            }
          }
        } catch (Exception e) {
          // ignore transient adb errors during boot
        }
      }

      retryCount++;
      try {
        logService.addLog("Waiting for device to come online...");
        TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for device");
      }
    }
    throw new RuntimeException("Local emulator did not come online within timeout.");
  }

  private void installDroidbotApp() {
    String droidbotAppPath = "C:\\Users\\kaspa\\Documents\\Coding\\Bachellor-project-2025\\droidbot\\droidbot\\resources";
    try {
      List<String> installCommand = List.of("adb", "-s", getDeviceSerial(), "install", "-r", droidbotAppPath);
      String result = shellCommandService.runCommandSync(installCommand);
      logService.addLog("DroidbotApp install result: " + result);
      // Optionally, grant permissions if needed
      shellCommandService.runCommandSync(List.of("adb", "-s", getDeviceSerial(), "shell", "pm", "grant", "io.github.ylimit.droidbotapp", "android.permission.WRITE_SECURE_SETTINGS"));
    } catch (Exception e) {
      logService.addLog("Failed to install DroidbotApp: " + e.getMessage());
    }
  }

  private void performCleanup(String packageName) {
    logService.addLog("Performing cleanup...");
    try {
      // 1. Kill the specific app
      if (packageName != null) {
        shellCommandService.runCommandSync(List.of("adb", "shell", "am", "force-stop", packageName));
        logService.addLog("App " + packageName + " stopped."); // NEED TO KNOW ACTUAL PACKAGE NAME
      }

      // 2. Kill the emulator (Optional: ONLY if you want to close the device)
      // shellCommandService.runCommandSync(List.of("adb", "emu", "kill"));
      // logService.addLog("Emulator shutting down.");

    } catch (Exception e) {
      logService.addLog("Cleanup failed: " + e.getMessage());
    }
  }

//  private void waitForDeviceOnline() {
//    int maxRetries = 60; // Increase retries for longer wait
//    int retryCount = 0;
//    while (retryCount < maxRetries) {
//      try {
//        // Check if device is listed
//        ProcessBuilder pb = new ProcessBuilder("adb", "devices");
//        Process process = pb.start();
//        process.waitFor();
//        boolean deviceFound = false;
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//          String line;
//          while ((line = reader.readLine()) != null) {
//            if (line.contains("localhost:5555") && line.contains("device")) {
//              deviceFound = true;
//              break;
//            }
//          }
//        }
//        if (deviceFound) {
//          // Additional check: Ensure boot is completed
//          ProcessBuilder bootCheck = new ProcessBuilder("adb", "-s", "localhost:5555", "shell", "getprop", "sys.boot_completed");
//          Process bootProcess = bootCheck.start();
//          bootProcess.waitFor();
//          try (BufferedReader bootReader = new BufferedReader(new InputStreamReader(bootProcess.getInputStream()))) {
//            String bootLine = bootReader.readLine();
//            if ("1".equals(bootLine != null ? bootLine.trim() : null)) {
//              logService.addLog("Device is online and fully booted");
//              return;
//            }
//          }
//        }
//      } catch (Exception e) {
//        logService.addLog("Error checking device status: " + e.getMessage());
//      }
//      retryCount++;
//      try {
//        Thread.sleep(3000); // Wait 3 seconds before retrying
//      } catch (InterruptedException e) {
//        Thread.currentThread().interrupt();
//      }
//    }
//    throw new RuntimeException("Device did not come online after " + maxRetries + " retries");
//  }
//
//  private boolean isDeviceOnline() {
//    try {
//      ProcessBuilder pb = new ProcessBuilder("adb", "devices");
//      Process process = pb.start();
//      process.waitFor();
//      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//        String line;
//        while ((line = reader.readLine()) != null) {
//          if (line.contains("localhost:5555") && line.contains("device")) {
//            return true;
//          }
//        }
//      }
//    } catch (Exception e) {
//      // Ignore
//    }
//    return false;
//  }

  private void updateJobStatus(Job job, String statusName) {
    JobStatus status = jobStatusRepository.findByName(statusName);
    if (status == null) {
      // Create if not exists (Auto-seeding)
      status = JobStatus.builder().name(statusName).build();
      status = jobStatusRepository.save(status);
    }
    job.setStatus(status);
    jobRepository.save(job);
  }
}
