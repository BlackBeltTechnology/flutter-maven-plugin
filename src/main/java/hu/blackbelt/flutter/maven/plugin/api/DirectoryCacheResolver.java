package hu.blackbelt.flutter.maven.plugin.api;

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

public class DirectoryCacheResolver implements CacheResolver {

  private final File cacheDirectory;

  public DirectoryCacheResolver(File cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  @Override
  public File resolve(CacheDescriptor cacheDescriptor) {
    if (!cacheDirectory.exists()) {
      cacheDirectory.mkdirs();
    }

    StringBuilder filename = new StringBuilder()
        .append(cacheDescriptor.getName())
        .append("-")
        .append(cacheDescriptor.getVersion());
    filename.append(".").append(cacheDescriptor.getExtension());
    return new File(cacheDirectory, filename.toString());
  }

}
