package android.tester.backend.controllers;

import android.tester.backend.dtos.StartTestRequest;
import android.tester.backend.entities.*;
import android.tester.backend.enums.Role;
import android.tester.backend.repositories.*;
import android.tester.backend.services.logging.LogService;
import android.tester.backend.services.shellCommands.ShellCommandService;
import android.tester.backend.services.storage.StorageService;
import android.tester.backend.services.test.TestExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/android")
@RequiredArgsConstructor
public class AndroidController {

  private final ShellCommandService shellCommandService;
  private final LogService logService;
  private final StorageService storageService;
  private final TestExecutionService testExecutionService;
  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final TestRunRepository testRunRepository;
  private final JobRepository jobRepository;
  private final JobStatusRepository jobStatusRepository;
  private final TestDeviceRepository testDeviceRepository;

  private final LogService logger;

  private static final UUID FAKE_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @GetMapping("/logs")
  public List<String> getConsoleLogs() {
    return logService.getLogs();
  }

  @PostMapping("/adb/restart")
  public String restartAdb() {
    shellCommandService.runCommandSync(Arrays.asList("adb", "kill-server"));
    return shellCommandService.runCommandSync(Arrays.asList("adb", "start-server"));
  }

  @GetMapping("/devices")
  public String getDevices() {
    return shellCommandService.runCommandSync(Arrays.asList("adb", "devices"));
  }

  @PostMapping("/emulator/start")
  public String startEmulator() {
    // Warning: This assumes 'emulator' is in your Windows Environmental PATH
    List<String> cmd = Arrays.asList("emulator", "-avd", "TestDevice", "-no-snapshot-load");

    shellCommandService.runCommandAsync(cmd, null);

    return "Emulator launch command issued. Check server logs for boot progress.";
  }

  @PostMapping("/emulator/stop")
  public String stopEmulator() {
    List<String> cmd = Arrays.asList("adb", "-s", "emulator-5544", "emu", "kill");
    return "Killed emulator.";
  }

  @PostMapping("/droidbot/run")
  public String runDroidbot() {
    // Windows paths must be escaped with double backslashes
    String apkPath = "C:\\DroidBot\\com.neumorphic.calculator_1.apk";
    String outPath = "C:\\DroidBot\\calc2";

    List<String> cmd = Arrays.asList(
      "py", "-3.7-32", "-m", "droidbot.start",
      "-a", apkPath,
      "-o", outPath,
      "-is_emulator",
      "-timeout", "60"
    );

//    List<String> cleanUpCmd = Arrays.asList("adb", "uninstall", "com.neumorphic.calculator");
    List<String> cleanUpCmd = Arrays.asList("adb", "kill-server");

    shellCommandService.runCommandAsync(cmd, cleanUpCmd);

    return "DroidBot run started for APK: " + apkPath;
  }

  @PostMapping("/upload")
  public ResponseEntity<String> uploadApp(@RequestParam("file") MultipartFile file) {
    try {
      String path = storageService.saveApk(file, FAKE_USER_ID);

      Application application = Application.builder()
        .name(file.getOriginalFilename())
        .packageName("unknown.package") // Should extract from APK using a tool in future
        .version("1.0") // Should extract
        .apkFile(path)
        .versionNumber(0)
        .created(OffsetDateTime.now())
        .build();

      application = applicationRepository.saveAndFlush(application);

      logger.addLog("Uploaded file: " + path + " as application ID: " + application.getId());

      return ResponseEntity.ok("File uploaded successfully: " + application.getId());
    } catch (IOException e) {
      return ResponseEntity.internalServerError().body("Failed to upload: " + e.getMessage());
    }
  }

  @PostMapping("/test/start")
  public ResponseEntity<String> startTest(@RequestBody StartTestRequest request) {
    logger.addLog("Starting test for user: " + request.toString());
    // 1. Validate User
    User user = userRepository.findById(FAKE_USER_ID).orElseGet(() -> {
      User newUser = User.builder()
        .id(FAKE_USER_ID)
        .username("fakeuser")
        .password("password")
//        .salt("abc")
        .email("test@email.com")
//        .version(0)
        .role(Role.USER)
        .created(OffsetDateTime.now())
        .build();
      return userRepository.save(newUser);
    });

    logger.addLog("Starting test for user: " + user.getUsername());

    // 2. Validate App
    Application app = applicationRepository.findById(request.appId())
      .orElseThrow(() -> new RuntimeException("App not found"));

    logger.addLog("Starting test for app: " + app.getName());

    // 3. Create TestRun
    TestRun testRun = TestRun.builder()
      .user(user)
      .runDate(OffsetDateTime.now())
      .description("Automated Test Run")
      .finished(false)
      .created(OffsetDateTime.now())
      .build();
    testRun = testRunRepository.save(testRun);

    logger.addLog("Created test run: " + testRun.getId());

    // 4. Create TestDevice
    TestDevice testDevice = TestDevice.builder()
      .testRun(testRun)
      .name(request.deviceName())
      .resolution(request.resolution())
      .screenSize(request.screenSize())
      .tablet(request.tablet())
      .created(OffsetDateTime.now())
      .build();
    testDeviceRepository.save(testDevice);

    logger.addLog("Created test device: " + testDevice.getId());

    // 5. Create JobStatus (Pending)
    JobStatus status = jobStatusRepository.findByName("Pending");
    if (status == null) {
      status = JobStatus.builder().name("Pending").build();
      status = jobStatusRepository.save(status);
    }

    logger.addLog("Created job status: " + status.getId());

    // 6. Create Job
    Job job = Job.builder()
      .application(app)
      .testRun(testRun)
      .status(status)
      .created(OffsetDateTime.now())
      .build();
    job = jobRepository.save(job);

    logger.addLog("Created job: " + job.getId());

    // 7. Start Exection
    testExecutionService.executeJob(job.getId());

    logger.addLog("Started test execution for job: " + job.getId());

    return ResponseEntity.ok("Test started. Job ID: " + job.getId());
  }
}
