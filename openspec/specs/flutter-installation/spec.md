# Flutter Installation Specification

## Purpose

The flutter-installation capability handles downloading, extracting, and verifying Flutter SDK installations within a Maven project. It supports both binary archive downloads (version-specific) and Git-based installations (channel-specific), with automatic platform detection and file permission management.

## Architecture

The installation is orchestrated by `InstallFlutterMojo` which delegates to `FlutterInstaller` via `FlutterPluginFactory`. The installer uses `FileDownloader` for HTTP downloads, `ArchiveExtractor` for archive processing, `CacheResolver` for caching downloaded archives, and JGit for Git-based installations. Installation is thread-safe via synchronized blocks.

Key classes:
- `InstallFlutterMojo` — Maven mojo entry point (`install-flutter` goal)
- `FlutterInstaller` — Core installation logic with builder-style configuration
- `FlutterPluginFactory` — Factory creating installer instances with shared config
- `InstallConfig` / `DefaultInstallConfig` — Installation directory and cache configuration

## Requirements

### Requirement: Version-based installation from archive

The plugin SHALL download and install a specific Flutter version from a binary archive when `flutterVersion` is specified.

#### Scenario: Install Flutter on Linux with specific version
- **GIVEN** `flutterVersion` is set to `3.10.5` and the OS is Linux
- **WHEN** the `install-flutter` goal executes
- **THEN** the plugin downloads `stable/linux/flutter_linux_3.10.5-stable.tar.xz` from the download root
- **AND** extracts the archive to a temporary directory
- **AND** moves the extracted `flutter/` directory to the install directory
- **AND** sets executable permissions on `bin/flutter`, `bin/dart`, and shell scripts
- **AND** runs `flutter doctor` to verify the installation

#### Scenario: Install Flutter on Windows with specific version
- **GIVEN** `flutterVersion` is set to `3.10.5` and the OS is Windows
- **WHEN** the `install-flutter` goal executes
- **THEN** the plugin downloads the ZIP archive variant
- **AND** verifies that `flutter/bin/flutter.bat` exists in the extracted content

### Requirement: Channel-based installation from Git

The plugin SHALL clone the Flutter Git repository and switch to the specified channel when no `flutterVersion` is provided.

#### Scenario: Install Flutter from stable channel via Git
- **GIVEN** `flutterVersion` is not set and `flutterChannel` is `stable`
- **WHEN** the `install-flutter` goal executes
- **THEN** the plugin clones `https://github.com/flutter/flutter.git` to the install directory
- **AND** runs `flutter channel stable`
- **AND** runs `flutter upgrade`
- **AND** verifies the installation via version check

#### Scenario: Reuse existing Git clone
- **GIVEN** a `.git` directory already exists in the install directory
- **WHEN** the `install-flutter` goal executes
- **THEN** the plugin opens the existing Git repository instead of cloning
- **AND** stashes any dirty state before proceeding

### Requirement: Skip installation when already installed

The plugin SHALL skip installation when the correct Flutter version or channel is already installed.

#### Scenario: Matching version already installed
- **GIVEN** Flutter `3.10.5` is already installed at the install directory
- **AND** `flutterVersion` is set to `3.10.5`
- **WHEN** the `install-flutter` goal executes
- **THEN** the plugin detects the installed version via `flutter --version`
- **AND** skips the installation process

#### Scenario: Version mismatch triggers reinstall
- **GIVEN** Flutter `3.10.4` is installed
- **AND** `flutterVersion` is set to `3.10.5`
- **WHEN** the `install-flutter` goal executes
- **THEN** the plugin proceeds with downloading and installing version `3.10.5`

### Requirement: Corrupted archive recovery

The plugin SHALL delete corrupted archives and allow retry on next build.

#### Scenario: Incomplete download causes EOFException
- **GIVEN** a cached archive file exists but is truncated/corrupted
- **WHEN** the archive extraction throws an `EOFException`
- **THEN** the plugin deletes the corrupted archive file
- **AND** deletes the temporary extraction directory
- **AND** throws an `ArchiveExtractionException` (next build will re-download)

### Requirement: Thread-safe installation

The plugin SHALL synchronize installation to prevent concurrent installations from conflicting.

#### Scenario: Parallel Maven build with multiple modules
- **GIVEN** Maven is running with `-T 4` (4 threads)
- **AND** multiple modules depend on the `install-flutter` goal
- **WHEN** multiple threads attempt to install Flutter simultaneously
- **THEN** only one thread executes the installation at a time (synchronized on static lock)

### Requirement: Proxy-aware installation

The plugin SHALL use Maven proxy settings for both HTTP downloads and Git operations.

#### Scenario: Download through corporate proxy
- **GIVEN** Maven `settings.xml` has an HTTPS proxy configured
- **WHEN** the plugin downloads the Flutter archive
- **THEN** the `DefaultFileDownloader` routes the request through the configured proxy
- **AND** uses proxy credentials if authentication is required

#### Scenario: Git clone through proxy
- **GIVEN** a proxy is configured in `ProxyConfig`
- **WHEN** the plugin clones from Git
- **THEN** a custom `ProxySelector` is installed that routes the Flutter Git URL through the proxy

### Requirement: Authentication support

The plugin SHALL support server credentials from Maven `settings.xml` for authenticated downloads.

#### Scenario: Download from authenticated repository
- **GIVEN** `serverId` is set to `my-server`
- **AND** Maven `settings.xml` contains encrypted credentials for `my-server`
- **WHEN** the plugin downloads Flutter
- **THEN** it decrypts the server credentials using `SettingsDecrypter`
- **AND** passes `username` and `password` to the `FileDownloader`
