package hu.blackbelt.flutter.maven.plugin.flutter;

import hu.blackbelt.flutter.maven.plugin.api.TaskRunnerException;

import java.util.Map;

public interface FlutterTaskRunner {
    void execute(String args, Map<String,String> environment) throws TaskRunnerException;
}
