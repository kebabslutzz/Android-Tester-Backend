package android.tester.backend.controllers;

import android.tester.backend.services.logging.LogService;
import android.tester.backend.services.shellCommands.ShellCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/android")
@RequiredArgsConstructor
public class AndroidController {

  private final ShellCommandService shellCommandService;
  private final LogService logService;

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
}
