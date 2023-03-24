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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;

import java.io.File;

public class RepositoryCacheResolver implements CacheResolver {

  private static final String GROUP_ID = "hu.blackbelt.flutter.maven/plugin";
  private final RepositorySystemSession repositorySystemSession;

  public RepositoryCacheResolver(RepositorySystemSession repositorySystemSession) {
    this.repositorySystemSession = repositorySystemSession;
  }

  @Override
  public File resolve(CacheDescriptor cacheDescriptor) {
    LocalRepositoryManager manager = repositorySystemSession.getLocalRepositoryManager();
    File localArtifact = new File(
        manager.getRepository().getBasedir(),
        manager.getPathForLocalArtifact(createArtifact(cacheDescriptor))
    );
    return localArtifact;
  }

  private DefaultArtifact createArtifact(CacheDescriptor cacheDescriptor) {
    String version = cacheDescriptor.getVersion(); //.replaceAll("^v", "");

    DefaultArtifact artifact;

    artifact = new DefaultArtifact(
        GROUP_ID,
        cacheDescriptor.getName(),
        cacheDescriptor.getExtension(),
        version
    );

    return artifact;
  }
}
