package android.tester.backend.services.shellCommands;

import android.tester.backend.services.logging.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ShellCommandService {

  private final LogService logService;

  public String runCommandSync(List<String> command) {
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(true);

    try {
      Process process = builder.start();
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      int exitCode = process.waitFor();
      String result = "Exit Code: " + exitCode + "\nOutput:\n" + output;

      logService.addLog("Ran Sync Command: " + command);
      logService.addLog(result);

      return result;
    } catch (IOException | InterruptedException e) {
      return "Error: " + e.getMessage();
    }
  }

  @Async
  public CompletableFuture<String> runCommandAsync(List<String> command, List<String> cleanupCommand) {
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(true);

    try {
      String startMsg = "Starting Async Command: " + String.join(" ", command);
      logService.addLog(startMsg);

      Process process = builder.start();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          logService.addLog(line);
        }
      }
      logService.addLog("Async command finished: " + command.get(0));

      if (cleanupCommand != null && !cleanupCommand.isEmpty()) {
        logService.addLog("Running cleanup command: " + String.join(" ", cleanupCommand));
        runCommandSync(cleanupCommand);
      }

      return CompletableFuture.completedFuture("Process finished");
    } catch (IOException e) {
      String errorMsg = "Error starting process: " + e.getMessage();
      logService.addLog(errorMsg);
      return CompletableFuture.completedFuture(errorMsg);
    }
  }
}
