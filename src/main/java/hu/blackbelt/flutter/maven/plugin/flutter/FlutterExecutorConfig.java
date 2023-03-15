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
