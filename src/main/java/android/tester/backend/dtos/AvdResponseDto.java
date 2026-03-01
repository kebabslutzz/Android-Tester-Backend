package android.tester.backend.dtos;

public record AvdResponseDto(
  String name,
  String device,
  String path,
  String target,
  String osVersion,
  String tagAbi
) {
}
