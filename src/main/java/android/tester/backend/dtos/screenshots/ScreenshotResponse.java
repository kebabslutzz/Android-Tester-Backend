package android.tester.backend.dtos.screenshots;

import java.awt.image.BufferedImage;
import java.util.UUID;

public record ScreenshotResponse(
  UUID screenshotId,
  UUID testDeviceId,
  UUID appId,
  BufferedImage image
) {
}
