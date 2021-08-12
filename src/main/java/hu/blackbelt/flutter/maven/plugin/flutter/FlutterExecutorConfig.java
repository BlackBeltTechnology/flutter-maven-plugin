package hu.blackbelt.flutter.maven.plugin.flutter;

import java.io.File;

public interface FlutterExecutorConfig {
  File getFlutterPath();
  File getInstallDirectory();
  File getWorkingDirectory();
  Platform getPlatform();
}

final class DefaultFlutterExecutorConfig implements FlutterExecutorConfig {

  private static final String FLUTTER_WINDOWS = "\\bin\\flutter.bat";
  private static final String FLUTTER_DEFAULT = "/bin/flutter";

  private final InstallConfig installConfig;

  public DefaultFlutterExecutorConfig(InstallConfig installConfig) {
    this.installConfig = installConfig;
  }

  @Override
  public File getFlutterPath() {
    String flutterExecutable = getPlatform().isWindows() ? FLUTTER_WINDOWS : FLUTTER_DEFAULT;
    return new File(installConfig.getInstallDirectory() + flutterExecutable);
  }

  @Override
  public File getInstallDirectory() {
    return installConfig.getInstallDirectory();
  }
  
  @Override
  public File getWorkingDirectory() {
    return installConfig.getWorkingDirectory();
  }

  @Override
  public Platform getPlatform() {
    return installConfig.getPlatform();
  }
}