package hu.blackbelt.flutter.maven.plugin.api;

public final class InstallationException extends FlutterException {
    public InstallationException(String message){
        super(message);
    }
    public InstallationException(String message, Throwable cause){
        super(message, cause);
    }
}
