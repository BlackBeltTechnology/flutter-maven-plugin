package hu.blackbelt.flutter.maven.plugin.api;

public final class DownloadException extends Exception {
    public DownloadException(String message) {
        super(message);
    }

    DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
