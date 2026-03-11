# Flutter Execution Specification

## Purpose

The flutter-execution capability runs Flutter CLI commands within a Maven build lifecycle. It wraps the Flutter binary with proper PATH setup, proxy configuration, and environment variable management, supporting both sequential (synchronized) and parallel execution modes.

## Architecture

The execution chain flows from `FlutterMojo` through `FlutterPluginFactory` to `DefaultFlutterTaskRunner`, which delegates to `FlutterExecutor` and ultimately `ProcessExecutor`. The `FlutterMojo` handles incremental build detection via the Plexus `BuildContext`.

Key classes:
- `FlutterMojo` — Maven mojo entry point (`flutter` goal, default phase: `generate-resources`)
- `DefaultFlutterTaskRunner` — Parses arguments, builds proxy env vars, executes Flutter commands
- `FlutterExecutor` — Wraps `ProcessExecutor` with Flutter binary path on PATH
- `ProcessExecutor` — Low-level process execution via Apache Commons Exec
- `ArgumentsParser` — Splits string arguments into argument lists

## Requirements

### Requirement: Execute Flutter CLI commands

The plugin SHALL execute arbitrary Flutter CLI commands with the locally installed Flutter binary.

#### Scenario: Default pub get execution
- **GIVEN** Flutter is installed at `${basedir}/.flutter`
- **AND** no `arguments` parameter is specified
- **WHEN** the `flutter` goal executes
- **THEN** the plugin runs `flutter pub get` in the working directory
- **AND** redirects stdout/stderr to the Maven logger

#### Scenario: Custom Flutter command
- **GIVEN** `arguments` is set to `build web --release`
- **WHEN** the `flutter` goal executes
- **THEN** the plugin runs `flutter build web --release`
- **AND** fails the build if the exit code is non-zero

### Requirement: Incremental build support

The plugin SHALL skip execution when `pubspec.yaml` has not changed during incremental builds.

#### Scenario: No changes to pubspec.yaml
- **GIVEN** the build is incremental (`BuildContext.isIncremental()` returns true)
- **AND** `pubspec.yaml` has not been modified since the last build
- **WHEN** the `flutter` goal executes
- **THEN** the plugin logs "Skipping flutter pub get as pubspec.yaml unchanged"
- **AND** does not invoke the Flutter CLI

#### Scenario: pubspec.yaml modified
- **GIVEN** `pubspec.yaml` has been modified
- **WHEN** the `flutter` goal executes
- **THEN** the plugin runs the Flutter command normally

### Requirement: Synchronized execution by default

The plugin SHALL synchronize Flutter command execution to prevent concurrent access issues, unless parallel mode is explicitly enabled.

#### Scenario: Default sequential execution
- **GIVEN** `parallel` is `false` (the default)
- **WHEN** multiple modules execute the `flutter` goal concurrently
- **THEN** execution is serialized via a static lock object shared across all `FlutterMojo` instances

#### Scenario: Parallel execution enabled
- **GIVEN** `parallel` is set to `true`
- **WHEN** the `flutter` goal executes
- **THEN** no synchronization lock is acquired
- **AND** multiple modules can run Flutter commands concurrently

### Requirement: Proxy environment variable injection

The plugin SHALL pass Maven proxy settings as environment variables to Flutter commands.

#### Scenario: Proxy configured in Maven
- **GIVEN** `flutterInheritsProxyConfigFromMaven` is `true` (default)
- **AND** an HTTPS proxy is configured in Maven settings
- **WHEN** the `flutter` goal executes
- **THEN** `https-proxy` and `http-proxy` environment variables are set with the proxy URI
- **AND** `noproxy` is set with non-proxy host patterns

#### Scenario: Proxy inheritance disabled
- **GIVEN** `flutterInheritsProxyConfigFromMaven` is `false`
- **WHEN** the `flutter` goal executes
- **THEN** no proxy environment variables are added

### Requirement: Non-zero exit code handling

The plugin SHALL fail the Maven build when a Flutter command returns a non-zero exit code.

#### Scenario: Flutter command fails
- **GIVEN** a Flutter command returns exit code `1`
- **WHEN** the `DefaultFlutterTaskRunner.execute()` method processes the result
- **THEN** a `TaskRunnerException` is thrown with the error code
- **AND** the build fails with a `MojoFailureException`

#### Scenario: Test failure ignored during test phase
- **GIVEN** `maven.test.failure.ignore` is `true`
- **AND** the current lifecycle phase is `test`
- **WHEN** a Flutter command fails
- **THEN** the error is logged but the build continues

### Requirement: Proxy password masking in logs

The plugin SHALL mask proxy passwords when logging command arguments.

#### Scenario: Command with proxy credentials
- **GIVEN** a Flutter command includes a proxy argument with embedded credentials
- **WHEN** the command is logged
- **THEN** the password portion of the proxy URI is replaced with `***`
