package hu.blackbelt.flutter.maven.plugin.flutter;

import hu.blackbelt.flutter.maven.plugin.api.ProcessExecutionException;
import hu.blackbelt.flutter.maven.plugin.api.Utils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlutterExecutor {
    private final ProcessExecutor executor;

    public FlutterExecutor(FlutterExecutorConfig config, List<String> arguments, Map<String, String> additionalEnvironment){
        String flutter = config.getFlutterPath().getAbsolutePath();

        List<String> localPaths = new ArrayList<String>();
        localPaths.add(config.getFlutterPath().getParent());
        this.executor = new ProcessExecutor(
            config.getWorkingDirectory(),
            localPaths,
            Utils.prepend(flutter, arguments),
            config.getPlatform(),
            additionalEnvironment);
    }

    public String executeAndGetResult(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndGetResult(logger);
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndRedirectOutput(logger);
    }
}
