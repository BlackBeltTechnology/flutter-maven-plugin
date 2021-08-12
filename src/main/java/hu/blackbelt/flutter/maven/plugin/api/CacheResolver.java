package hu.blackbelt.flutter.maven.plugin.api;

import java.io.File;

public interface CacheResolver {
  File resolve(CacheDescriptor cacheDescriptor);
}
