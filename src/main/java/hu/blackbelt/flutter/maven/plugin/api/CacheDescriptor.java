package hu.blackbelt.flutter.maven.plugin.api;

public class CacheDescriptor {

  private final String name;
  private final String version;
  private final String extension;

  public CacheDescriptor(String name, String version, String extension) {
    this.name = name;
    this.version = version;
    this.extension = extension;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getExtension() {
    return extension;
  }
}
