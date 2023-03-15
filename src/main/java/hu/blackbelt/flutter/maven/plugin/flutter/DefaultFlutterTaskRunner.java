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

import hu.blackbelt.flutter.maven.plugin.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class DefaultFlutterTaskRunner implements FlutterTaskRunner {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String DS = "//";
    private static final String AT = "@";

    private final String taskName;
    private final String taskLocation;
    private final ArgumentsParser argumentsParser;
    private final FlutterExecutorConfig executorConfig;
    private final Map<String, String> additionalVariables;

    static final String TASK_NAME = "flutter";

    static Map<String, String> buildProxyVariables(ProxyConfig proxyConfig) {
        Map<String, String> arguments = new HashMap<>();

        if(!proxyConfig.isEmpty()){
            ProxyConfig.Proxy proxy = null;
            proxy = proxyConfig.getSecureProxy();

            if(proxy == null){
                proxy = proxyConfig.getInsecureProxy();
            }

            arguments.put("https-proxy", proxy.getUri().toString());
            arguments.put("http-proxy", proxy.getUri().toString());

            final String nonProxyHosts = proxy.getNonProxyHosts();
            if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
                final String[] nonProxyHostList = nonProxyHosts.split("\\|");
                for (String nonProxyHost: nonProxyHostList) {
                    arguments.put("noproxy", nonProxyHost.replace("*", ""));
                }
            }
        }

        return arguments;
    }

    public DefaultFlutterTaskRunner(FlutterExecutorConfig config, ProxyConfig proxyConfig) {
        this(config, TASK_NAME, config.getFlutterPath().getAbsolutePath(), Collections.emptyList(), buildProxyVariables(proxyConfig));
    }

    public DefaultFlutterTaskRunner(FlutterExecutorConfig config, String taskLocation) {
        this(config, taskLocation, Collections.<String>emptyList(), Collections.emptyMap());
    }

    public DefaultFlutterTaskRunner(FlutterExecutorConfig config, String taskName, String taskLocation) {
        this(config, taskName, taskLocation, Collections.<String>emptyList(), Collections.emptyMap());
    }

    public DefaultFlutterTaskRunner(FlutterExecutorConfig config, String taskLocation, List<String> additionalArguments, Map<String, String> additionalVariables) {
        this(config, getTaskNameFromLocation(taskLocation), taskLocation, additionalArguments, additionalVariables);
    }

    public DefaultFlutterTaskRunner(FlutterExecutorConfig config, String taskName, String taskLocation, List<String> additionalArguments, Map<String, String> additionalVariables) {
        this.executorConfig = config;
        this.taskName = taskName;
        this.taskLocation = taskLocation;
        this.argumentsParser = new ArgumentsParser(additionalArguments);
        this.additionalVariables = additionalVariables;
    }

    private static String getTaskNameFromLocation(String taskLocation) {
        return taskLocation.replaceAll("^.*/([^/]+)(?:\\.js)?$","$1");
    }

    public final void execute(String args, Map<String, String> environment) throws TaskRunnerException {
        final String absoluteTaskLocation = getAbsoluteTaskLocation();
        final List<String> arguments = getArguments(args);
        logger.info("Running " + taskToString(taskName, arguments) + " in " + executorConfig.getWorkingDirectory());

        Map<String, String> allEnvironment = new HashMap<>();
        if (additionalVariables != null && additionalVariables.size() > 0) {
            allEnvironment.putAll(additionalVariables);
        }
        if (environment != null && environment.size() > 0) {
            allEnvironment.putAll(environment);
        }

        // Utils.prepend(absoluteTaskLocation, arguments)
        try {
            final int result = new FlutterExecutor(executorConfig, arguments, allEnvironment).executeAndRedirectOutput(logger);
            if (result != 0) {
                throw new TaskRunnerException(taskToString(taskName, arguments) + " failed. (error code " + result + ")");
            }
        } catch (ProcessExecutionException e) {
            throw new TaskRunnerException(taskToString(taskName, arguments) + " failed.", e);
        }
    }

    private String getAbsoluteTaskLocation() {
        String location = Utils.normalize(taskLocation);
        if (Utils.isRelative(taskLocation)) {
            File taskFile = new File(executorConfig.getWorkingDirectory(), location);
            if (!taskFile.exists()) {
                taskFile = new File(executorConfig.getInstallDirectory(), location);
            }
            location = taskFile.getAbsolutePath();
        }
        return location;
    }

    private List<String> getArguments(String args) {
        return argumentsParser.parse(args);
    }

    private static String taskToString(String taskName, List<String> arguments) {
        List<String> clonedArguments = new ArrayList<>(arguments);
        for (int i = 0; i < clonedArguments.size(); i++) {
            final String s = clonedArguments.get(i);
            final boolean maskMavenProxyPassword = s.contains("proxy=");
            if (maskMavenProxyPassword) {
                final String bestEffortMaskedPassword = maskPassword(s);
                clonedArguments.set(i, bestEffortMaskedPassword);
            }
        }
        return "'" + taskName + " " + Utils.implode(" ", clonedArguments) + "'";
    }

    private static String maskPassword(String proxyString) {
        String retVal = proxyString;
        if (proxyString != null && !"".equals(proxyString.trim())) {
            boolean hasSchemeDefined = proxyString.contains("http:") || proxyString.contains("https:");
            boolean hasProtocolDefined = proxyString.contains(DS);
            boolean hasAtCharacterDefined = proxyString.contains(AT);
            if (hasSchemeDefined && hasProtocolDefined && hasAtCharacterDefined) {
                final int firstDoubleSlashIndex = proxyString.indexOf(DS);
                final int lastAtCharIndex = proxyString.lastIndexOf(AT);
                boolean hasPossibleURIUserInfo = firstDoubleSlashIndex < lastAtCharIndex;
                if (hasPossibleURIUserInfo) {
                    final String userInfo = proxyString.substring(firstDoubleSlashIndex + DS.length(), lastAtCharIndex);
                    final String[] userParts = userInfo.split(":");
                    if (userParts.length > 0) {
                        final int startOfUserNameIndex = firstDoubleSlashIndex + DS.length();
                        final int firstColonInUsernameOrEndOfUserNameIndex = startOfUserNameIndex + userParts[0].length();
                        final String leftPart = proxyString.substring(0, firstColonInUsernameOrEndOfUserNameIndex);
                        final String rightPart = proxyString.substring(lastAtCharIndex);
                        retVal = leftPart + ":***" + rightPart;
                    }
                }
            }
        }
        return retVal;
    }
}
