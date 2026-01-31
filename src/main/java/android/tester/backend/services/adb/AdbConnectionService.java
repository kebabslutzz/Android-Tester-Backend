package android.tester.backend.services.adb;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AdbConnectionService {

  @Value("${adb.host}")
  private String adbHost;

  @Value("${adb.port}")
  private String adbPort;

  @PostConstruct
  public void connectToContainer() {
    String target = adbHost + ":" + adbPort;
    System.out.println("🔌 Attempting to connect to Android Container at: " + target);

    try {
      ProcessBuilder pb = new ProcessBuilder("adb", "connect", target);
      pb.inheritIO();
      Process process = pb.start();

      int exitCode = process.waitFor();
      if (exitCode == 0) {
        System.out.println("Successfully connected to Docker Android!");
      } else {
        System.err.println("Failed to connect. Is the Docker container running?");
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
