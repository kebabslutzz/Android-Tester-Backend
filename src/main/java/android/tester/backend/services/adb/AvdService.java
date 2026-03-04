package android.tester.backend.services.adb;

import android.tester.backend.dtos.AvdResponseDto;
import android.tester.backend.dtos.android.AvailableAPILevelsDto;
import android.tester.backend.dtos.android.AvailableDeviceImageDto;
import android.tester.backend.exceptions.NotFoundException;
import android.tester.backend.services.shellCommands.ShellCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AvdService {

  private final ShellCommandService shellCommandService;

  @Cacheable("system_images")
  public List<AvdResponseDto> getAvailableAvds() {
    // Use cmd /c and avdmanager.bat for Windows compatibility
    String rawOutput = shellCommandService.runCommandSync(Arrays.asList("cmd", "/c", "avdmanager.bat", "list", "avd"));
    List<AvdResponseDto> avds = new ArrayList<>();

    // Regex adjusted for Windows line endings (\r?\n) and the "Output:" prefix
    Pattern pattern = Pattern.compile(
      "Name:\\s*(.*?)\\r?\\n\\s*Device:\\s*(.*?)\\r?\\n\\s*Path:\\s*(.*?)\\r?\\n\\s*Target:\\s*(.*?)\\r?\\n\\s*Based on:\\s*(.*?)\\s*Tag/ABI:\\s*(.*?)(?=\\r?\\n\\s*Sdcard|\\r?\\n---|\\z)",
      Pattern.DOTALL
    );

    Matcher matcher = pattern.matcher(rawOutput);
    while (matcher.find()) {
      avds.add(new AvdResponseDto(
        matcher.group(1).trim(),
        matcher.group(2).trim(),
        matcher.group(3).trim(),
        matcher.group(4).trim(),
        matcher.group(5).trim().replace("\"", ""),
        matcher.group(6).trim()
      ));
    }

    if (avds.isEmpty()) {
      throw new NotFoundException("No AVDs found in output: " + rawOutput);
    }

    return avds;
  }


  public List<AvailableDeviceImageDto> getInstalledSystemImages() {
    List<AvailableDeviceImageDto> images = new ArrayList<>();

    // Command: sdkmanager --list_installed
    // Note: On Windows, "sdkmanager.bat" might be needed if not in PATH, but assuming PATH based on context.
    List<String> cmd = List.of("cmd.exe", "/c", "sdkmanager", "--list_installed");

    String output = shellCommandService.runCommandSync(cmd);

    // Helper to parse lines
    String[] lines = output.split("\n");
    for (String line : lines) {
      line = line.trim();

      // Look for lines starting with "system-images;"
      if (line.startsWith("system-images;")) {
        // Example line: system-images;android-33;google_apis;x86_64 | 14 | ...
        // We only need the first part (the path ID)
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

    // Command: avdmanager list target
    List<String> cmd = List.of("cmd.exe", "/c", "avdmanager", "list", "target");

    String output = shellCommandService.runCommandSync(cmd);

    // Output format often looks like:
    // id: 1 or "android-33"
    //      Name: Android API 33
    //      Type: Platform
    //      API level: 33
    //      Revision: 1

    // Simple regex to extract API ids if the user wants strictly target IDs
    Pattern pattern = Pattern.compile("id: \\d+ or \"(android-\\d+)\"");
    Matcher matcher = pattern.matcher(output);

    while (matcher.find()) {
      targets.add(new AvailableAPILevelsDto(matcher.group(1)));
    }

    return targets;
  }

  private AvailableDeviceImageDto parseSystemImageString(String imageStr) {
    // Format: system-images;android-{API};{TAG};{ABI}
    // Example: system-images;android-30;google_apis;x86

    try {
      String[] parts = imageStr.split(";");
      if (parts.length < 4) return null;

      String apiPart = parts[1]; // android-30
      String tag = parts[2];     // google_apis or default
      String abi = parts[3];     // x86 or x86_64

      int apiLevel = Integer.parseInt(apiPart.replace("android-", ""));

      // Construct a readable name
      // Android 10 = API 29, Android 11 = API 30, etc.
      // A simple display string: "API 30 (google_apis x86)"
      String displayName = "Android API " + apiLevel + " (" + tag + " " + abi + ")";

      return new AvailableDeviceImageDto(displayName, apiLevel, imageStr);
    } catch (Exception e) {
      // Logic to handle parsing errors gracefully (skip weird lines)
      return null;
    }
  }
}
