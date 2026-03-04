package android.tester.backend.services.job;

import android.tester.backend.dtos.android.AvailableAPILevelsDto;
import android.tester.backend.dtos.android.AvailableDeviceImageDto;
import android.tester.backend.entities.*;
import android.tester.backend.exceptions.AvdCreationException;
import android.tester.backend.exceptions.AvdImageNotFoundException;
import android.tester.backend.exceptions.DeviceBootException;
import android.tester.backend.repositories.JobRepository;
import android.tester.backend.repositories.JobStatusRepository;
import android.tester.backend.repositories.ScreenshotRepository;
import android.tester.backend.repositories.TestDeviceRepository;
import android.tester.backend.services.adb.AdbConnectionService;
import android.tester.backend.services.adb.AvdService;
import android.tester.backend.services.legacy.LegacyAnalysisService;
import android.tester.backend.services.logging.LogService;
import android.tester.backend.services.shellCommands.ShellCommandService;
import android.tester.backend.services.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JobService {

  private final ShellCommandService shellCommandService;
  private final LogService logService;
  private final StorageService storageService;
  private final JobRepository jobRepository;
  private final JobStatusRepository jobStatusRepository;
  private final ScreenshotRepository screenshotRepository;
  private final LegacyAnalysisService legacyAnalysisService;
  private final AdbConnectionService adbConnectionService;
  private final TestDeviceRepository testDeviceRepository;
  private final AvdService avdService;

  //  private static final String EMULATOR_SERIAL = "emulator-5554";
  private static final String DEFAULT_SYSTEM_IMAGE = "system-images;android-33;google_apis;x86_64";

  @Async
  public void executeJob(UUID jobId) {
    Optional<Job> jobOpt = jobRepository.findById(jobId);
    if (jobOpt.isEmpty()) {
      logService.addLog("Error: Job not found for ID: " + jobId);
      return;
    }
    Job job = jobOpt.get();
    logService.addLog("Started test execution for job: " + job.getId());

    //    String deviceSerial = EMULATOR_SERIAL;
    String deviceSerial = "emulator-5554";

    try {
      // 1. Setup AVD (Create unique AVD for this job)
      updateJobStatus(job, "Creating AVD");
      TestDevice testDevice = job.getTestRun().getTestDevice();
      String avdName = testDevice.getName();

      // Ensure no stale AVD exists
      deleteAvd(avdName); // Cleanup just in case
      createAvd(avdName, testDevice);
      updateAvdConfig(avdName,
        Integer.parseInt(testDevice.getResolution().split("x")[0]),
        Integer.parseInt(testDevice.getResolution().split("x")[1]),
        testDevice.getDpi(),
        testDevice.getTablet()
      );


      // 2. Start Emulator
      updateJobStatus(job, "Starting Emulator");
      startEmulator(avdName);
      waitForDeviceFullyBooted(deviceSerial);

      // 3. Install App
      updateJobStatus(job, "Installing App");
      Application app = job.getApplication();
      String apkPath = app.getApkPath(); // Use the absolute path stored in DB
      if (apkPath == null) throw new RuntimeException("APK Path is null");

      installAppUnderTest(deviceSerial, apkPath);
      installDroidbotApp(deviceSerial);

      // 4. Run Droidbot
      updateJobStatus(job, "Running Test");
      String outputDir = storageService.createTestRunOutputDirectory(
        job.getTestRun().getUser().getId(),
        app.getPackageName(),
        job.getTestRun().getId()
      );

      // We run Droidbot via python command
      // outputDir needs to be absolute
      String absOutputDir = new File(outputDir).getAbsolutePath();

      List<String> droidbotCmd = new ArrayList<>();
      droidbotCmd.add("py");
      droidbotCmd.add("-3.7-32");
      droidbotCmd.add("-m");
      droidbotCmd.add("droidbot.start");
      droidbotCmd.add("-d");
      droidbotCmd.add(deviceSerial);
      droidbotCmd.add("-a");
      droidbotCmd.add(apkPath);
      droidbotCmd.add("-o");
      droidbotCmd.add(absOutputDir);
      droidbotCmd.add("-is_emulator");
      droidbotCmd.add("-timeout");
      droidbotCmd.add("200"); // Short timeout for testing
      droidbotCmd.add("-keep_app");

      logService.addLog("Starting Droidbot: " + droidbotCmd);
      shellCommandService.runCommandSync(droidbotCmd);

      // 5. Analyze Results
      updateJobStatus(job, "Analyzing");
      logService.addLog("Analyzing results...");
      processScreenshots(job, absOutputDir, apkPath);

      // 6. Cleanup & Finish
      updateJobStatus(job, "Done");
      performCleanup(deviceSerial, app.getPackageName(), avdName);

    } catch (Exception e) {
      e.printStackTrace();
      logService.addLog("Job Failed: " + e.getMessage());
      updateJobStatus(job, "Error");
      // Attempt cleanup even on error
      try {
        performCleanup(deviceSerial, job.getApplication().getPackageName(), job.getTestRun().getTestDevice().getName());
      } catch (Exception ex) {
        logService.addLog("Cleanup failed after error: " + ex.getMessage());
      }
    }
  }

  // --- HELPER METHODS ---

  private void installAppUnderTest(String serial, String apkPath) {
    logService.addLog("Installing APK: " + apkPath);
    String res = shellCommandService.runCommandSync(List.of("adb", "-s", serial, "install", "-r", "-g", apkPath));
    if (res.contains("Failure")) {
      throw new RuntimeException("Failed to install APK: " + res);
    }
  }

  private void installDroidbotApp(String serial) {
    // Hardcoded path for environment
    String droidbotAppPath = "C:\\DroidBot\\droidbot_app.apk";
    String droidbotAppCopyPath = "C:\\DroidBot\\droidbot_app_copy.apk";

    File fCopy = new File(droidbotAppCopyPath);

    if (!fCopy.exists()) {
      File f = new File(droidbotAppPath);
      if (!f.exists()) {
        logService.addLog("Warning: Droidbot app helper not found at " + droidbotAppPath);
        return;
      }
      try {
        Files.copy(f.toPath(), fCopy.toPath());
        logService.addLog("Created copy of Droidbot app at " + droidbotAppCopyPath);
      } catch (IOException e) {
        // If file still doesn't exist (e.g. race condition didn't resolve it), log error
        if (!fCopy.exists()) {
          logService.addLog("Failed to copy Droidbot app: " + e.getMessage());
          return;
        }
      }
    }

    shellCommandService.runCommandSync(List.of("adb", "-s", serial, "install", "-r", "-g", droidbotAppCopyPath));
  }

  private void performCleanup(String serial, String packageName, String avdName) {
    logService.addLog("Performing cleanup...");
    // Stop app
    if (packageName != null) {
      shellCommandService.runCommandSync(List.of("adb", "-s", serial, "shell", "am", "force-stop", packageName));
    }
    // Kill emulator
    shellCommandService.runCommandSync(List.of("adb", "-s", serial, "emu", "kill"));

    // Give it a moment to release locks
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
    }

    // Delete AVD
    deleteAvd(avdName);
  }

  private void startLocalEmulator() {
    // ... reused logic if needed, but we use startEmulator(avdName) specific
  }

  private void startEmulator(String avdName) {
    // "emulator" must be in PATH
    List<String> cmd = new ArrayList<>();
    cmd.add("emulator");
    cmd.add("-avd");
    cmd.add(avdName);
    cmd.add("-no-snapshot-load");
    cmd.add("-no-boot-anim");
    // Detached launch
    shellCommandService.runCommandAsync(cmd, null);
    logService.addLog("Emulator launch command issued for " + avdName);
  }

  /**
   * We need to wait until the device is strictly "device" in adb devices
   * AND sys.boot_completed is 1.
   */
  private boolean isDeviceReady(String serial) {
    // 1. Check adb devices
    String out = shellCommandService.runCommandSync(List.of("adb", "devices"));
    if (!out.contains(serial + "\tdevice")) {
      return false;
    }
    // 2. Check boot property
    try {
      String boot = shellCommandService.runCommandSync(List.of("adb", "-s", serial, "shell", "getprop", "sys.boot_completed"));
      // Relaxed check: trim and check if it contains "1".
      // Output often contains newlines or specific ADB formatting.
      return boot != null && boot.trim().contains("1");
    } catch (Exception e) {
      return false;
    }
  }

  private void waitForDeviceFullyBooted(String serial) {
    logService.addLog("Waiting for " + serial + " to boot...");
    int maxRetries = 40; // 40 * 3s = 120s
    for (int i = 0; i < maxRetries; i++) {
      if (isDeviceReady(serial)) {
        logService.addLog("Device " + serial + " is online and ready.");
        // Extra sleep to ensure launcher is settled
        try {
          logService.addLog("Waiting a minute for device to settle..");
          Thread.sleep(60000);
        } catch (InterruptedException e) {
        }
        return;
      }
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted");
      }
    }
    throw new DeviceBootException("Device " + serial + " failed to boot within timeout.");
  }

  private void processScreenshots(Job job, String outputDir, String apkPath) {
    File statesDir = new File(outputDir, "states");
    if (!statesDir.exists()) {
      logService.addLog("No states directory found at " + statesDir.getAbsolutePath());
      return;
    }

    logService.addLog("Processing screenshots...");

    File[] files = statesDir.listFiles((d, name) -> name.endsWith(".png"));
    if (files == null) return;

    // Fake/Legacy app object
    android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application legacyApp =
      new android.tester.backend.legacy.java.edu.ktu.screenshotanalyser.database.DataBase.Application(
        0, ".", true, apkPath, job.getApplication().getPackageName(), job.getApplication().getPackageName()
      );

    for (File func : files) {
      logService.addLog("Processing screenshot: " + func.getName());
      String jsonName = func.getName().replace(".png", ".json").replace("screen", "state");
      File jsonFile = new File(statesDir, jsonName);
      if (jsonFile.exists()) {
        ScreenShot screenShot = ScreenShot.builder()
          .fileName(func.getName())
          .testDevice(job.getTestRun().getTestDevice())
          .application(job.getApplication())
          .created(OffsetDateTime.now())
          .build();
        screenShot = screenshotRepository.save(screenShot);

        logService.addLog("Analyzing screenshot: " + screenShot.getId());

        legacyAnalysisService.analyzeScreenshot(
          screenShot,
          func,
          jsonFile,
          legacyApp,
          statesDir,
          new ArrayList<>(),
          null
        );
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

  private void createAvd(String avdName, TestDevice device) {
    // 1. Determine System Image (Validate against installed)
    String systemImage = device.getSystemImage();
    String encSystemImage = systemImage;

    // Validate if the requested image actually exists
    if (systemImage != null && !systemImage.isBlank()) {
      boolean imageExists = getInstalledSystemImages().stream()
        .anyMatch(img -> img.systemImage().equals(encSystemImage));

      if (!imageExists) {
        logService.addLog("Requested image " + encSystemImage + " not found. Falling back or failing.");
        throw new AvdImageNotFoundException("The requested system image is not installed: " + encSystemImage);
      }
    } else {
      systemImage = DEFAULT_SYSTEM_IMAGE; // Fallback
    }

    logService.addLog("Creating AVD '" + avdName + "' using image: " + systemImage);

    // 2. Run avdmanager create
    // echo "no" | avdmanager create avd -n Name -k "system-image" --force
    // Windows requires careful piping. We'll use ShellCommandService which handles basic waiting,
    // but piping input needs support. Usually 'avdmanager' asks "Do you wish to create a custom hardware profile? [no]"
    // passing --device "pixel" or similar might skip it, but echoing "no" is safest.

    // Using `cmd /c echo no | avdmanager ...` might work.
    List<String> cmd = new ArrayList<>();
    cmd.add("cmd.exe");
    cmd.add("/c");
    // e.g. "echo no | avdmanager create avd -n <name> -k <image> --force"
    String internalCmd = String.format("echo no | avdmanager create avd -n \"%s\" -k \"%s\" --force", avdName, systemImage);
    cmd.add(internalCmd);

    String output = shellCommandService.runCommandSync(cmd);
    if (output.contains("Error:")) {
      throw new AvdCreationException("Failed to create AVD: " + output);
    }
  }

  private void updateAvdConfig(String avdName, int width, int height, Integer dpi, Boolean isTablet) {
    // Config location: ~/.android/avd/<avdName>.avd/config.ini
    // On Windows: C:\Users\<User>\.android\avd\<avdName>.avd\config.ini

    String userHome = System.getProperty("user.home");
    File avdDir = new File(userHome, ".android/avd/" + avdName + ".avd");
    File configFile = new File(avdDir, "config.ini");

    if (!configFile.exists()) {
      logService.addLog("Warning: Config file not found at " + configFile.getAbsolutePath());
      return;
    }

    try {
      List<String> lines = Files.readAllLines(configFile.toPath());
      List<String> newLines = new ArrayList<>();
      boolean hwLcdHeightFound = false;
      boolean hwLcdWidthFound = false;
      boolean hwLcdDensityFound = false;

      for (String line : lines) {
        if (line.startsWith("hw.lcd.height")) {
          newLines.add("hw.lcd.height=" + height);
          hwLcdHeightFound = true;
        } else if (line.startsWith("hw.lcd.width")) {
          newLines.add("hw.lcd.width=" + width);
          hwLcdWidthFound = true;
        } else if (line.startsWith("hw.lcd.density")) {
          if (dpi != null) {
            newLines.add("hw.lcd.density=" + dpi);
            hwLcdDensityFound = true;
          } else {
            newLines.add(line);
          }
        } else {
          newLines.add(line);
        }
      }

      if (!hwLcdHeightFound) newLines.add("hw.lcd.height=" + height);
      if (!hwLcdWidthFound) newLines.add("hw.lcd.width=" + width);
      if (dpi != null && !hwLcdDensityFound) newLines.add("hw.lcd.density=" + dpi);

      // Write back
      try (FileWriter writer = new FileWriter(configFile)) {
        for (String line : newLines) {
          writer.write(line + System.lineSeparator());
        }
      }

    } catch (IOException e) {
      logService.addLog("Failed to update AVD config: " + e.getMessage());
    }
  }

  private void deleteAvd(String avdName) {
    // avdmanager delete avd -n <name>
    List<String> cmd = List.of("cmd.exe", "/c", "avdmanager", "delete", "avd", "-n", avdName);
    shellCommandService.runCommandSync(cmd);
  }

  @Cacheable("system_images")
  public List<AvailableDeviceImageDto> getInstalledSystemImages() {
    List<AvailableDeviceImageDto> images = new ArrayList<>();

    // Correct command: sdkmanager --list_installed
    // avdmanager list image is NOT valid.
    List<String> cmd = List.of("cmd.exe", "/c", "sdkmanager", "--list_installed");

    String output;
    try {
      output = shellCommandService.runCommandSync(cmd);
    } catch (Exception e) {
      logService.addLog("Failed to list images: " + e.getMessage());
      return Collections.emptyList();
    }

    String[] lines = output.split("\n");
    for (String line : lines) {
      line = line.trim();
      // sdkmanager output format:
      // system-images;android-33;google_apis;x86_64   | 14           | Google APIs Intel x86 Atom_64 System Image
      if (line.startsWith("system-images;")) {
        String pathId = line.split("\\|")[0].trim();
        AvailableDeviceImageDto dto = parseSystemImageString(pathId);
        if (dto != null) {
          images.add(dto);
        }
      }
    }
    return images;
  }

  @Cacheable("avd_targets")
  public List<AvailableAPILevelsDto> getAvailableTargets() {
    List<AvailableAPILevelsDto> targets = new ArrayList<>();

    // Make command explicit
    List<String> cmd = List.of("cmd.exe", "/c", "avdmanager", "list", "target");

    String output;
    try {
      output = shellCommandService.runCommandSync(cmd);
    } catch (Exception e) {
      return Collections.emptyList();
    }

    // Output can include "Loading local repository..." noise.
    // We look specifically for the regex `id: \d+ or "android-XX"`
    Pattern pattern = Pattern.compile("id: \\d+ or \"(android-\\d+)\"");
    Matcher matcher = pattern.matcher(output);

    while (matcher.find()) {
      targets.add(new AvailableAPILevelsDto(matcher.group(1)));
    }

    return targets;
  }

  private AvailableDeviceImageDto parseSystemImageString(String imageStr) {
    // Format: system-images;android-{API};{TAG};{ABI}
    try {
      String[] parts = imageStr.split(";");
      if (parts.length < 4) return null;

      String apiPart = parts[1]; // android-30
      String tag = parts[2];     // google_apis
      String abi = parts[3];     // x86_64

      int apiLevel = Integer.parseInt(apiPart.replace("android-", ""));
      String displayName = "Android API " + apiLevel + " (" + tag + " " + abi + ")";

      return new AvailableDeviceImageDto(displayName, apiLevel, imageStr);
    } catch (Exception e) {
      return null;
    }
  }

}
