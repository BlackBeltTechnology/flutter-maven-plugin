package hu.blackbelt.flutter.maven.plugin.api;

public interface ArchiveExtractor {
    void extract(String archive, String destinationDirectory) throws ArchiveExtractionException;
}

