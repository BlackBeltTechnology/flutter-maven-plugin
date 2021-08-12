package hu.blackbelt.flutter.maven.plugin.flutter;

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
