package hu.blackbelt.flutter.maven.plugin.api;

public class ArchiveExtractionException extends Exception {

    ArchiveExtractionException(String message) {
        super(message);
    }

    ArchiveExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
