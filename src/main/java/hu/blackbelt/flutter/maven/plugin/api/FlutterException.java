package hu.blackbelt.flutter.maven.plugin.api;

public class FlutterException extends Exception {

  public FlutterException(String message) {
    super(message);
  }

  public  FlutterException(String message, Throwable cause){
    super(message, cause);
  }
}
