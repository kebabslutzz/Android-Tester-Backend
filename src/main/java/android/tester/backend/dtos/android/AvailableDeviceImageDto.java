package android.tester.backend.dtos.android;

public record AvailableDeviceImageDto(
  String name,
  int apiLevel,
  String systemImage
) {
}
