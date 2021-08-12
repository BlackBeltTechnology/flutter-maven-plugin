package hu.blackbelt.flutter.maven.plugin.api;

public class TaskRunnerException extends FlutterException {
    public TaskRunnerException(String message) {
        super(message);
    }

    public TaskRunnerException(String message, Throwable cause){
        super(message, cause);
    }
}
