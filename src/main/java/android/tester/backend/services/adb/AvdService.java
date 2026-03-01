package android.tester.backend.services.adb;

import android.tester.backend.dtos.AvdResponseDto;
import android.tester.backend.exceptions.NotFoundException;
import android.tester.backend.services.shellCommands.ShellCommandService;
import lombok.RequiredArgsConstructor;
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
}
