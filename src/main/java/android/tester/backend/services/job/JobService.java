package android.tester.backend.services.job;

import android.tester.backend.entities.*;
import android.tester.backend.exceptions.NotFoundException;
import android.tester.backend.repositories.*;
import android.tester.backend.services.adb.AdbConnectionService;
import android.tester.backend.services.legacy.LegacyAnalysisService;
import android.tester.backend.services.logging.LogService;
import android.tester.backend.services.shellCommands.ShellCommandService;
import android.tester.backend.services.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JobService {


  private final JobRepository jobRepository;
  private final JobStatusRepository jobStatusRepository;
  private final ShellCommandService shellCommandService;
  private final StorageService storageService;
  private final LegacyAnalysisService legacyAnalysisService;
  private final ScreenshotRepository screenshotRepository;
  private final LogService logService;
  private final ApplicationRepository applicationRepository;
  private final TestRunRepository testRunRepository;
  private final TestDeviceRepository testDeviceRepository;
  private final AdbConnectionService adbConnectionService;


  // Ideally, this should come from the database based on the request
  private static final String LOCAL_AVD_NAME = "TestDevice";
  private static final String EMULATOR_SERIAL = "emulator-5554"; // Default for first emulator


  @Async
//  @Transactional // Ensure the session stays open for the duration of the async task
  public void executeJob(UUID jobId) {
    Optional<Job> jobOpt = jobRepository.findById(jobId);
    if (jobOpt.isEmpty()) {
      throw new NotFoundException("Job not found for ID: " + jobId);
    }
    Job job = jobOpt.get();
    logService.addLog("Started test execution for job: " + job.getId());

    String deviceSerial = EMULATOR_SERIAL;

    try {
      // --- PHASE 1: DEVICE PREPARATION ---
      updateJobStatus(job, "Booting Device");

      boolean ready = isDeviceReady(deviceSerial);

      if (!ready) {
        logService.addLog("Device " + deviceSerial + " not ready/online. Attempting restart...");

        // Kill server to refresh connections
        shellCommandService.runCommandSync(List.of("adb", "kill-server"));
        shellCommandService.runCommandSync(List.of("adb", "start-server"));

        // Launch AVD
        startLocalEmulator();

        // Wait specifically for boot
        waitForDeviceFullyBooted(deviceSerial);
      } else {
        logService.addLog("Device " + deviceSerial + " is already online. Verifying boot status...");
        // Even if online, ensure it's fully booted
        waitForDeviceFullyBooted(deviceSerial);
      }

      // --- PHASE 2: APP INSTALLATION ---
      updateJobStatus(job, "Installing Apps");

      Application app = job.getApplication();
//      String actualApkPath = app.getApkFile();
      String actualApkPath = app.getApkPath();

      if (actualApkPath == null || actualApkPath.isEmpty()) {
        throw new RuntimeException("APK file path not found in database.");
      }

      installDroidbotApp(deviceSerial);
      installAppUnderTest(deviceSerial, actualApkPath);


      // --- PHASE 3: DROIDBOT EXECUTION ---
      updateJobStatus(job, "Running Droidbot");

      TestRun testRun = job.getTestRun();
      String outputDir = storageService.createTestRunOutputDirectory(
        testRun.getUser().getId(),
        app.getPackageName(),
        testRun.getId()
      );

      List<String> command = new ArrayList<>();
      // Adjust python command based on your environment (py -3.7-32 used in your previous logs)
      command.add("py");
      command.add("-3.7-32");
      command.add("-m");
      command.add("droidbot.start");
      command.add("-d");
      command.add(deviceSerial);
      command.add("-a");
      command.add(actualApkPath);
      command.add("-o");
      command.add(outputDir);
      command.add("-is_emulator");
      command.add("-timeout");
      command.add("120");
      command.add("-keep_app");

      logService.addLog("Starting Droidbot on " + deviceSerial);
      shellCommandService.runCommandSync(command);
      logService.addLog("Droidbot execution finished.");


      // --- PHASE 4: ANALYSIS ---
      updateJobStatus(job, "Analyzing Results");
      processScreenshots(job, outputDir, actualApkPath);

      // --- DONE ---
      updateJobStatus(job, "Completed");

    } catch (Exception e) {
      logService.addLog("Job Failed: " + e.getMessage());
      e.printStackTrace();
      updateJobStatus(job, "Failed");
    } finally {
      // --- PHASE 5: CLEANUP ---
      performCleanup(deviceSerial, job.getApplication().getPackageName());
    }
  }

  // --- HELPER METHODS ---

  private void installAppUnderTest(String serial, String apkPath) {
    logService.addLog("Installing App Under Test...");
    // -g grants permissions, -t allows test APKs, -r replaces existing
    List<String> installCmd = List.of("adb", "-s", serial, "install", "-t", "-r", "-g", apkPath);
    String result = shellCommandService.runCommandSync(installCmd);

    if (!result.contains("Success")) {
      throw new RuntimeException("Failed to install APK. Output: " + result);
    }
    logService.addLog("App installed successfully.");
  }

  private void installDroidbotApp(String serial) {
    String droidbotAppPath = "C:\\Users\\kaspa\\Documents\\Coding\\Bachellor-project-2025\\droidbot\\droidbot\\resources\\droidbot_app.apk";

    File f = new File(droidbotAppPath);
    if (!f.exists()) {
      logService.addLog("WARNING: Droidbot helper APK not found at " + droidbotAppPath);
      return;
    }

    logService.addLog("Ensuring Droidbot Helper is installed...");
    List<String> installCommand = List.of("adb", "-s", serial, "install", "-r", "-g", droidbotAppPath);
    shellCommandService.runCommandSync(installCommand);

    shellCommandService.runCommandSync(List.of("adb", "-s", serial, "shell", "pm", "grant", "io.github.ylimit.droidbotapp", "android.permission.WRITE_SECURE_SETTINGS"));
  }

  private void performCleanup(String serial, String packageName) {
    logService.addLog("Performing cleanup for package: " + packageName);
    try {
      if (packageName != null) {
        shellCommandService.runCommandSync(List.of("adb", "-s", serial, "uninstall", packageName));
        logService.addLog("App " + packageName + " uninstalled.");
      }
      // Optional: stop emulator
      // shellCommandService.runCommandSync(List.of("adb", "-s", serial, "emu", "kill"));
    } catch (Exception e) {
      logService.addLog("Cleanup failed: " + e.getMessage());
    }
  }

  private void startLocalEmulator() {
    List<String> cmd = List.of(
      "emulator",
      "-avd", LOCAL_AVD_NAME,
      "-no-snapshot-load",
      "-no-boot-anim",
      "-netdelay", "none",
      "-netspeed", "full"
    );

    try {
      shellCommandService.runCommandAsync(cmd, null);
      logService.addLog("Emulator launch command issued.");
    } catch (Exception e) {
      throw new RuntimeException("Failed to start emulator: " + e.getMessage());
    }
  }

  /**
   * Checks if device is listed in ADB and responds to shell commands with specific output.
   */
  private boolean isDeviceReady(String serial) {
    try {
      // 1. Check if ADB sees it as "device"
      List<String> devicesCmd = List.of("adb", "devices");
      String output = shellCommandService.runCommandSync(devicesCmd);
      if (!output.contains(serial + "\tdevice") && !output.contains(serial + " device")) {
        return false;
      }

      // 2. Check sys.boot_completed
      // We check if output contains "1" because sometimes it has whitespace
      String bootProp = shellCommandService.runCommandSync(List.of("adb", "-s", serial, "shell", "getprop", "sys.boot_completed"));
      if (!bootProp.trim().contains("1")) {
        return false;
      }

      // 3. Check Package Manager (verifies system services are running)
      // FIX: Don't check strictly for "package:android" as newer android versions return paths like /system/framework/framework-res.apk
      // checking for just "package:" ensures PM is responding with a path
      String pmOutput = shellCommandService.runCommandSync(List.of("adb", "-s", serial, "shell", "pm", "path", "android"));
      return pmOutput.contains("package:");
    } catch (Exception e) {
      return false;
    }
  }

  private void waitForDeviceFullyBooted(String serial) {
    int maxRetries = 40; // Approx 40 * 5s = 200 seconds
    int retryCount = 0;

    logService.addLog("Waiting for device " + serial + " boot signals...");

    while (retryCount < maxRetries) {
      if (isDeviceReady(serial)) {
        logService.addLog("Device signals received. Stabilizing...");
        break;
      }

      retryCount++;
      try {
        if (retryCount % 5 == 0) logService.addLog("Still waiting for boot... (Attempt " + retryCount + ")");
        TimeUnit.SECONDS.sleep(5);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted waiting for device");
      }
    }

    if (retryCount >= maxRetries) {
      throw new RuntimeException("Timeout: Device " + serial + " failed to boot.");
    }

    // MANDATORY WAIT: Even after sys.boot_completed=1, the UI and Input Methods take time
    try {
      logService.addLog("Waiting 45s for UI/Input stabilization...");
      TimeUnit.SECONDS.sleep(45);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void processScreenshots(Job job, String outputDir, String apkPath) {
    File droidbotOutputFolder = new File(outputDir);
    File statesFolder = new File(droidbotOutputFolder, "states");

    if (statesFolder.exists() && statesFolder.isDirectory()) {
      File[] files = statesFolder.listFiles();
      if (files != null) {
        int processedCount = 0;
        for (File file : files) {
          String fileName = file.getName();
          if (fileName.endsWith(".png") && fileName.startsWith("screen_")) {

            String jsonFileName = fileName.replace("screen_", "state_").replace(".png", ".json");
            File layoutFile = new File(statesFolder, jsonFileName);

            if (layoutFile.exists()) {
              ScreenShot screenShot = ScreenShot.builder()
                .fileName(fileName)
                .testDevice(job.getTestRun().getTestDevice())
                .application(job.getApplication())
                .created(OffsetDateTime.now())
                .build();

              screenShot = screenshotRepository.save(screenShot);
              processedCount++;

              // Legacy Analysis context
              android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application legacyApp =
                new android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application(
                  0, ".", true, apkPath, job.getApplication().getPackageName(), job.getApplication().getPackageName()
                );

              legacyAnalysisService.analyzeScreenshot(
                screenShot, file, layoutFile, legacyApp, statesFolder, List.of(), null
              );
            }
          }
        }
        logService.addLog("Analyzed " + processedCount + " screenshots.");
      }
    }
  }

  private void updateJobStatus(Job job, String statusName) {
    JobStatus status = jobStatusRepository.findByName(statusName);
    if (status == null) {
      status = JobStatus.builder().name(statusName).build();
      status = jobStatusRepository.save(status);
    }
    job.setStatus(status);
    jobRepository.save(job);
  }

}
