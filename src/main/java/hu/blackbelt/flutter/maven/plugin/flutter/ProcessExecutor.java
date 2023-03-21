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
import org.apache.commons.exec.*;
import org.slf4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProcessExecutor {
    private final static String PATH_ENV_VAR = "PATH";

    private final Map<String, String> environment;
    private CommandLine commandLine;
    private final Executor executor;

    public ProcessExecutor(File workingDirectory, List<String> paths, List<String> command, Platform platform, Map<String, String> additionalEnvironment){
        this(workingDirectory, paths, command, platform, additionalEnvironment, 0);
    }

    public ProcessExecutor(File workingDirectory, List<String> paths, List<String> command, Platform platform, Map<String, String> additionalEnvironment, long timeoutInSeconds) {
        this.environment = createEnvironment(paths, platform, additionalEnvironment);
        this.commandLine = createCommandLine(command);
        this.executor = createExecutor(workingDirectory, timeoutInSeconds);
    }

    public String executeAndGetResult(final Logger logger) throws ProcessExecutionException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitValue = execute(logger, stdout, stderr);
        if (exitValue == 0) {
            try {
                return stdout.toString("UTF-8").trim();
            } catch (UnsupportedEncodingException e) {
                throw new ProcessExecutionException(stdout + " " + e.toString());
            }
        } else {
            throw new ProcessExecutionException(stdout + " " + stderr);
        }
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        OutputStream stdout = new LoggerOutputStream(logger, 0);
        return execute(logger, stdout, stdout);
    }

    private int execute(final Logger logger, final OutputStream stdout, final OutputStream stderr)
            throws ProcessExecutionException {
        logger.debug("Executing command line {}", commandLine);
        try {
            ExecuteStreamHandler streamHandler = new PumpStreamHandler(stdout, stderr, System.in);
            executor.setStreamHandler(streamHandler);

            int exitValue = executor.execute(commandLine, environment);
            logger.debug("Exit value {}", exitValue);

            return exitValue;
        } catch (ExecuteException e) {
            if (executor.getWatchdog() != null && executor.getWatchdog().killedProcess()) {
                throw new ProcessExecutionException("Process killed after timeout");
            }
            throw new ProcessExecutionException(e);
        } catch (IOException e) {
            throw new ProcessExecutionException(e);
        }
    }

    private CommandLine createCommandLine(List<String> command) {
        commandLine = new CommandLine(command.get(0));

        for (int i = 1;i < command.size();i++) {
            String argument = command.get(i);
            commandLine.addArgument(argument, false);
        }
        return commandLine;
    }
    private Map<String, String> createEnvironment(final List<String> paths, final Platform platform, final Map<String, String> additionalEnvironment) {
        final Map<String, String> environment = new HashMap<>(System.getenv());

        if (additionalEnvironment != null) {
            environment.putAll(additionalEnvironment);
        }

        if (platform.isWindows()) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                final String pathName = entry.getKey();
                if (PATH_ENV_VAR.equalsIgnoreCase(pathName)) {
                    final String pathValue = entry.getValue();
                    environment.put(pathName, extendPathVariable(pathValue, paths));
                }
            }
        } else {
            final String pathValue = environment.get(PATH_ENV_VAR);
            environment.put(PATH_ENV_VAR, extendPathVariable(pathValue, paths));
        }

        return environment;
    }

    private String extendPathVariable(final String existingValue, final List<String> paths) {
        final StringBuilder pathBuilder = new StringBuilder();
        for (final String path : paths) {
            pathBuilder.append(path).append(File.pathSeparator);
        }
        if (existingValue != null) {
            pathBuilder.append(existingValue).append(File.pathSeparator);
        }
        return pathBuilder.toString();
    }

    private Executor createExecutor(File workingDirectory, long timeoutInSeconds) {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(workingDirectory);
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());   // Fixes #41

        if (timeoutInSeconds > 0) {
            executor.setWatchdog(new ExecuteWatchdog(timeoutInSeconds * 1000));
        }

        return executor;
    }

    private static class LoggerOutputStream extends LogOutputStream {
        private final Logger logger;

        LoggerOutputStream(Logger logger, int logLevel) {
            super(logLevel);
            this.logger = logger;
        }

        @Override
        public final void flush() {
            // buffer processing on close() only
        }

        @Override
        protected void processLine(final String line, final int logLevel) {
            logger.info(line);
        }
    }
}
