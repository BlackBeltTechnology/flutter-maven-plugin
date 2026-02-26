# Platform Detection Specification

## Purpose

The platform-detection capability identifies the current operating system and CPU architecture at runtime to determine the correct Flutter SDK download URL, archive format, and binary name.

## Architecture

Three enums (`OS`, `Architecture`, `Channel`) define the supported platforms, and the `Platform` class combines them to generate download filenames and paths. Detection reads from Java system properties `os.name`, `os.arch`, and `os.version`.

Key classes:
- `OS` — Enum: `Windows`, `Mac`, `Linux`, `SunOS`; provides archive extension and codename
- `Architecture` — Enum: `x86`, `x64`, `ppc64le`, `s390x`, `arm64`, `armv7l`
- `Channel` — Enum: `stable`, `beta`, `dev`
- `Platform` — Combines OS, Architecture, and Channel; generates download URLs and filenames

## Requirements

### Requirement: Operating system detection

The `OS.guess()` method SHALL detect the current operating system from the `os.name` system property.

#### Scenario: Windows detection
- **GIVEN** `os.name` contains `"Windows"`
- **WHEN** `OS.guess()` is called
- **THEN** `OS.Windows` is returned

#### Scenario: macOS detection
- **GIVEN** `os.name` contains `"Mac"`
- **WHEN** `OS.guess()` is called
- **THEN** `OS.Mac` is returned

#### Scenario: Linux detection
- **GIVEN** `os.name` does not contain `"Windows"`, `"Mac"`, or `"SunOS"`
- **WHEN** `OS.guess()` is called
- **THEN** `OS.Linux` is returned

### Requirement: Architecture detection

The `Architecture.guess()` method SHALL detect the CPU architecture from system properties.

#### Scenario: ARM64 / aarch64
- **GIVEN** `os.arch` equals `"aarch64"`
- **WHEN** `Architecture.guess()` is called
- **THEN** `Architecture.arm64` is returned

#### Scenario: x86_64 / amd64
- **GIVEN** `os.arch` contains `"64"` and is not `"aarch64"` or `"ppc64le"` or `"s390x"`
- **WHEN** `Architecture.guess()` is called
- **THEN** `Architecture.x64` is returned

#### Scenario: ARMv7
- **GIVEN** `os.arch` equals `"arm"` and `os.version` contains `"v7"`
- **WHEN** `Architecture.guess()` is called
- **THEN** `Architecture.armv7l` is returned

### Requirement: Archive extension selection

The platform SHALL determine the archive format based on the operating system.

#### Scenario: Windows or macOS archive
- **GIVEN** the OS is `Windows` or `Mac`
- **WHEN** `getArchiveExtension()` is called
- **THEN** `"zip"` is returned

#### Scenario: Linux archive
- **GIVEN** the OS is `Linux`
- **WHEN** `getArchiveExtension()` is called
- **THEN** `"tar.xz"` is returned

### Requirement: Download filename generation

The platform SHALL generate correctly formatted download filenames for the Flutter release server.

#### Scenario: Linux stable download filename
- **GIVEN** the OS is `Linux`, channel is `stable`, and version is `3.10.5`
- **WHEN** `getFlutterDownloadFilename("3.10.5")` is called
- **THEN** `"stable/linux/flutter_linux_3.10.5-stable.tar.xz"` is returned

#### Scenario: Windows stable download filename
- **GIVEN** the OS is `Windows`, channel is `stable`, and version is `3.10.5`
- **WHEN** `getFlutterDownloadFilename("3.10.5")` is called
- **THEN** `"stable/windows/flutter_windows_3.10.5-stable.zip"` is returned

### Requirement: OS codename mapping

The platform SHALL map OS enum values to Flutter release server codenames.

#### Scenario: OS codename mapping
- **WHEN** `getCodename()` is called
- **THEN** `Mac` maps to `"macos"`, `Windows` maps to `"windows"`, `Linux` and `SunOS` map to `"linux"`

### Requirement: Platform convenience queries

The `Platform` class SHALL provide boolean methods for OS-specific logic branching.

#### Scenario: Windows check for binary selection
- **GIVEN** the detected OS is `Windows`
- **WHEN** `isWindows()` is called
- **THEN** `true` is returned (used to select `flutter.bat` vs `flutter`)
