package hu.blackbelt.flutter.maven.plugin.flutter;

/*-
 * #%L
 * flutter-maven-plugin
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
