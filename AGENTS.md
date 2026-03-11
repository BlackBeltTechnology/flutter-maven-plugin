# Flutter Maven Plugin - Project Documentation

## Project Overview


**Repository:** BlackBeltTechnology/flutter-maven-plugin
**License:** Apache License 2.0
**Java Version:** 21
**Build System:** Maven 3.9.4+ with Maven Plugin API

1. A Maven plugin that automates Flutter SDK installation locally within Maven projects — no global Flutter installation needed
2. Supports two installation methods: binary archive download (version-specific) or Git clone (channel-based: stable/beta/dev)
3. Executes arbitrary Flutter CLI commands (e.g., `pub get`, `build`, `doctor`) as Maven build steps with incremental build support
4. Provides multi-platform support (Windows, macOS, Linux) with automatic OS/architecture detection and appropriate binary/archive handling
5. Inherits Maven proxy configuration and repository credentials for use in corporate/firewalled environments

## Code Instructions

1. First think through the problem, read the codebase for relevant files.
2. Before you make any major changes, check in with me and I will verify the plan.
3. Please every step of the way just give me a high level explanation of what changes you made.
4. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
5. Maintain a documentation file that describes how the architecture of the app works inside and out.
6. Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
7. For implementation use TDD (Test-Driven Development): write or update tests first to define the expected behaviour, verify they fail, then write the minimal implementation to make them pass.
8. Use DRY (Don't Repeat Yourself): extract reusable logic into separate classes, utilities, or components. If the same pattern appears in multiple places, refactor it into a shared helper.

## Directory Structure

```
flutter-maven-plugin/
├── src/main/java/hu/blackbelt/flutter/maven/plugin/
│   ├── mojos/flutter/       # Maven Mojo entry points
│   ├── flutter/             # Core Flutter integration logic
│   ├── api/                 # Public interfaces, utilities, exceptions
│   ├── Log.java             # Base logging abstraction
│   └── MavenLog.java        # Maven-specific logging adapter
├── .github/workflows/       # GitHub Actions CI/CD pipelines
├── .mvn/                    # Maven wrapper and JVM configuration
├── openspec/                # OpenSpec configuration and specs
├── pom.xml                  # Project build definition
└── logback-test.xml         # Test logging configuration
```

## Core Modules

This is a single-module Maven plugin project. The source code is organized into three packages by responsibility:

### Maven Integration Layer

| Class | Type | Purpose |
|-------|------|---------|
| `AbstractFlutterMojo` | Abstract Mojo | Base class with skip logic, synchronized execution, test-phase handling, and error conversion |
| `InstallFlutterMojo` | Mojo (`install-flutter`) | Downloads or clones Flutter SDK; runs in `generate-resources` phase; thread-safe with static lock |
| `FlutterMojo` | Mojo (`flutter`) | Executes Flutter CLI commands; defaults to `pub get`; supports incremental builds via `pubspec.yaml` change detection |

### Core Flutter Logic

| Class | Type | Purpose |
|-------|------|---------|
| `FlutterPluginFactory` | Factory | Creates `FlutterInstaller` and `FlutterTaskRunner` instances with shared configuration |
| `FlutterInstaller` | Service | Handles archive download + extraction or Git clone + channel switch; sets file permissions; runs `flutter doctor` |
| `FlutterExecutor` | Service | Wraps `ProcessExecutor` with Flutter binary PATH setup; redirects output to logger |
| `DefaultFlutterTaskRunner` | Service | Parses arguments, builds proxy environment variables, delegates to `FlutterExecutor` |
| `Platform` / `OS` / `Architecture` | Value objects | Detects runtime OS (Windows/Mac/Linux/SunOS) and CPU architecture (x64/arm64/etc.); determines archive format and download filenames |
| `ProcessExecutor` | Service | Low-level process execution via Apache Commons Exec with environment variable and PATH management |

### API / Infrastructure

| Class | Type | Purpose |
|-------|------|---------|
| `FileDownloader` / `DefaultFileDownloader` | Interface + Impl | HTTP file downloads with proxy and credential support via Apache HttpClient |
| `ArchiveExtractor` / `DefaultArchiveExtractor` | Interface + Impl | Extracts ZIP, TAR.XZ, TAR.GZ, and MSI archives with path traversal protection |
| `CacheResolver` | Interface | Resolves cached artifacts by descriptor |
| `DirectoryCacheResolver` | Implementation | Filesystem-based cache in `<installDir>/cache/` |
| `RepositoryCacheResolver` | Implementation | Maven repository-based cache using `RepositorySystemSession` |
| `ProxyConfig` | Value object | Wraps Maven proxy settings; supports authentication, non-proxy host patterns, secure/insecure proxies |
| `MojoUtils` | Utility | Extracts proxy config from Maven session, decrypts server credentials, converts exceptions |
| `CacheDescriptor` | Value object | Identifies cached artifacts by name, version, and extension |
| `ArgumentsParser` | Utility | Parses string arguments into argument lists |

### Exception Hierarchy

All plugin exceptions extend `FlutterException`:

| Exception | Thrown When |
|-----------|------------|
| `InstallationException` | Flutter installation fails (download, extraction, verification) |
| `DownloadException` | HTTP download fails |
| `ArchiveExtractionException` | Archive extraction fails |
| `ProcessExecutionException` | External process execution fails |
| `TaskRunnerException` | Flutter task execution fails (non-zero exit code) |

## Technology Stack

### Core Technologies

- **Maven Plugin API 3.1.0** — Plugin lifecycle integration
- **Maven Plugin Annotations 3.3** — `@Mojo`, `@Parameter`, `@Component` annotations
- **Maven Core 3.2.3** — Session, settings, and decrypter access
- **Eclipse JGit 5.10.0** — Git clone/checkout for channel-based Flutter installation
- **Apache Commons Compress 1.20** — TAR.XZ and TAR.GZ extraction
- **Apache Commons Exec 1.3** — Cross-platform process execution
- **Apache HttpClient 4.5.13** — HTTP downloads with proxy/auth support
- **Semver4j 3.1.0** — Semantic version parsing and comparison
- **Plexus Build API 0.0.7** — Incremental build context for IDE integration
- **Lombok 1.18.34** — Boilerplate reduction (provided scope)

### Build & Quality

- **Maven Compiler Plugin 3.10.1** — Java 21 compilation
- **Maven Surefire Plugin 3.5.1** — Test execution with JaCoCo agent
- **JaCoCo 0.8.12** — Code coverage instrumentation and reporting
- **SonarQube 3.9.1** — Static analysis and quality metrics
- **Logback 1.5.12** — Test logging via SLF4J

## Build Commands

Maven wrapper (`./mvnw`) is available.

```bash
# Full build with tests
mvn clean install

# Run tests only
mvn clean test

# Build without tests
mvn clean install -DskipTests

# Debug logging
mvn -X clean test

# Run a single test class
mvn test -Dtest=TestClassName
```

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `modules` | Active unless `-DskipModules=true`; controls module activation |
| `sign-artifacts` | GPG-signs artifacts using `sign-maven-plugin` for release |
| `release-central` | Deploys to Maven Central via Sonatype OSSRH with nexus-staging |
| `release-judong` | Deploys to JUDO Nexus snapshot/release repository |
| `generate-github-asciidoc-diagrams` | Generates PNG diagrams from AsciiDoc/PlantUML sources |
| `update-source-code-license` | Updates Apache 2.0 license headers in source files |

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Single-module Maven plugin build definition |
| `.mvn/extensions.xml` | Maven extensions (Tycho, wagon) |
| `.mvn/jvm.config` | JVM memory settings (`-Xms1024m -Xmx2048m`) |
| `logback-test.xml` | SLF4J/Logback configuration for test execution |
| `.github/workflows/build.yml` | Main CI/CD pipeline (build, test, deploy, release) |
| `.github/workflows/release.yml` | Manual release initiation workflow |

## Development Environment

**Required:**
- Java 21 JDK ([Azul Zulu](https://www.azul.com/downloads/?version=java-21-lts&package=jdk) recommended)
- Maven 3.9.4+ (or use the included `./mvnw` wrapper)

## Git Workflow

- **Main Branch:** `develop`
- **Release Branch:** `master` (contains latest released version)
- **Versioning:** `1.0.2-SNAPSHOT` (semantic versioning with GitFlow)
- **Branch Naming:** `feature/JNG-NUMBER`, `bugfix/JNG-NUMBER`, `release/X.Y-betaN`, `hotfix/JNG-NUMBER`
- **Commit Rule:** Every commit must reference a JIRA ticket (`JNG-xxx`)
- **CI/CD:** GitHub Actions with automated build, deploy, merge, and release workflows

## Important Notes

1. **Thread safety is critical:** Both `InstallFlutterMojo` and `FlutterMojo` (when `parallel=false`) synchronize on a static lock object to prevent concurrent Flutter operations from conflicting. The `FlutterInstaller` also has its own internal lock.

2. **Two installation strategies exist:** When `flutterVersion` is specified, the plugin downloads a binary archive (ZIP on Windows/macOS, TAR.XZ on Linux). When only `flutterChannel` is specified, it uses JGit to clone the Flutter repository and switch to that channel.

3. **Incremental build support:** `FlutterMojo` checks whether `pubspec.yaml` has changed using the Plexus `BuildContext`. If unchanged during incremental builds, the Flutter command is skipped.

4. **Platform detection is automatic:** The `Platform` class detects OS and architecture at startup and determines the correct download URL, archive extension, and binary name (e.g., `flutter.bat` on Windows vs `flutter` on Unix).

5. **Proxy configuration is inherited:** The plugin reads Maven's proxy settings from `settings.xml`, decrypts credentials via `SettingsDecrypter`, and applies them to both HTTP downloads and JGit operations.

6. **Corrupted archive recovery:** If an archive extraction fails with `EOFException` (indicating an incomplete download), the plugin automatically deletes the corrupt archive so a fresh download occurs on the next build.

7. **File permissions are handled explicitly:** On Unix systems, the installer sets POSIX executable permissions on Flutter binaries. On systems with ACL-only support, it creates ACL entries with execute permission.

## Related Documentation

- [README.md](README.md) — Usage guide with configuration examples
- [CONTRIBUTING.md](CONTRIBUTING.md) — Development setup and contribution guidelines
- [.github/CIFLOW.md](.github/CIFLOW.md) — CI/CD pipeline and branching strategy
