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

import hu.blackbelt.flutter.maven.plugin.api.*;

import java.io.File;

public final class FlutterPluginFactory {
    
    private static final Platform defaultPlatform = Platform.guess();
    private static final String DEFAULT_CACHE_PATH = "cache";

    private final File workingDirectory;
    private final File installDirectory;
    private final File tempDirectory;

    private final CacheResolver cacheResolver;

    public FlutterPluginFactory(File workingDirectory, File installDirectory, File tempDirectory){
        this(workingDirectory, installDirectory, tempDirectory, getDefaultCacheResolver(installDirectory));
    }

    public FlutterPluginFactory(File workingDirectory, File installDirectory, File tempDirectory, CacheResolver cacheResolver){
        this.workingDirectory = workingDirectory;
        this.installDirectory = installDirectory;
        this.tempDirectory = tempDirectory;
        this.cacheResolver = cacheResolver;
    }

    public FlutterInstaller getFlutterInstaller(ProxyConfig proxy) {
        return new FlutterInstaller(getInstallConfig(), proxy, new DefaultArchiveExtractor(), new DefaultFileDownloader(proxy));
    }

    public FlutterTaskRunner getFlutterExecutor(ProxyConfig proxy) {
        return new DefaultFlutterTaskRunner(getExecutorConfig(), proxy);
    }

    private FlutterExecutorConfig getExecutorConfig() {
        return new DefaultFlutterExecutorConfig(getInstallConfig());
    }

    private InstallConfig getInstallConfig() {
        return new DefaultInstallConfig(installDirectory, tempDirectory, workingDirectory, cacheResolver, defaultPlatform);
    }

    private static final CacheResolver getDefaultCacheResolver(File root) {
        return new DirectoryCacheResolver(new File(root, DEFAULT_CACHE_PATH));
    }
}
