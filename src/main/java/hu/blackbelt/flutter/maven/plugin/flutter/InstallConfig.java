package hu.blackbelt.flutter.maven.plugin.flutter;

import hu.blackbelt.flutter.maven.plugin.api.CacheResolver;

import java.io.File;

public interface InstallConfig {
  File getInstallDirectory();
  File getTempDirectory();
  File getWorkingDirectory();
  CacheResolver getCacheResolver();
  Platform getPlatform();
}

final class DefaultInstallConfig implements InstallConfig {

  private final File installDirectory;
  private final File tempDirectory;
  private final File workingDirectory;
  private final CacheResolver cacheResolver;
  private final Platform platform;
  
  public DefaultInstallConfig(File installDirectory,
                              File tempDirectory,
                              File workingDirectory,
                              CacheResolver cacheResolver,
                              Platform platform) {
    this.installDirectory = installDirectory;
    this.tempDirectory = tempDirectory;
    this.workingDirectory = workingDirectory;
    this.cacheResolver = cacheResolver;
    this.platform = platform;
  }

  @Override
  public File getInstallDirectory() {
    return this.installDirectory;
  }

  @Override
  public File getTempDirectory() {
    return this.tempDirectory;
  }

  @Override
  public File getWorkingDirectory() {
    return this.workingDirectory;
  }
  
  public CacheResolver getCacheResolver() {
    return cacheResolver;
  }

  @Override
  public Platform getPlatform() {
    return this.platform;
  }

}