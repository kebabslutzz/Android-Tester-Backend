package android.tester.backend.dtos;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record StartTestRequest(
  @NotNull
  UUID appId,
//  String deviceName,
  @Pattern(regexp = "^(\\d{3,4})x(\\d{3,4})$", message = "Resolution must be in the format WIDTHxHEIGHT (e.g. 1080x1920)")
  String resolution,
  @DecimalMin(value = "2.8", message = "Screen size must be at least 2.8 inches")
  @DecimalMax(value = "13.0", message = "Screen size must be at most 13.0 inches")
  Double screenSize,
  Boolean tablet,
  @NotNull
  String systemImage,
  @NotNull
  Integer apiLevel,
  @Min(value = 120, message = "DPI must be at least 120")
  @Max(value = 640, message = "DPI must be at most 640")
  Integer dpi
) {
}
