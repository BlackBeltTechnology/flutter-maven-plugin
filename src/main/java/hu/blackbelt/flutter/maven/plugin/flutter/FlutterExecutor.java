package hu.blackbelt.flutter.maven.plugin.flutter;

/*-
 * #%L
 * flutter-maven-plugin
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
