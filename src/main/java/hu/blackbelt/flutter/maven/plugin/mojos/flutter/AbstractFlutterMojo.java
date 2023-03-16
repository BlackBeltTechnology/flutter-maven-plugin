package hu.blackbelt.flutter.maven.plugin.mojos.flutter;

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

import java.io.File;
import java.util.Map;

import hu.blackbelt.flutter.maven.plugin.api.MojoUtils;
import hu.blackbelt.flutter.maven.plugin.api.FlutterException;
import hu.blackbelt.flutter.maven.plugin.api.RepositoryCacheResolver;
import hu.blackbelt.flutter.maven.plugin.api.TaskRunnerException;
import hu.blackbelt.flutter.maven.plugin.flutter.FlutterPluginFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

public abstract class AbstractFlutterMojo extends AbstractMojo {

    public static final Object lock = new Object();

    @Component
    protected MojoExecution execution;

    /**
     * Whether you should skip while running in the test phase (default is false)
     */
    @Parameter(property = "skipTests", required = false, defaultValue = "false")
    protected Boolean skipTests;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @since 1.4
     */
    @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
    protected boolean testFailureIgnore;

    /**
     * The base directory for running all Flutter commands. (Usually the directory that contains pubspec.yaml)
     */
    @Parameter(defaultValue = "${basedir}", property = "flutter-working-directory", required = false)
    protected File workingDirectory;

    /**
     * The base directory for installing flutter.
     */
    @Parameter(defaultValue = "${basedir}/.flutter", property = "flutter-install-directory", required = false)
    protected File installDirectory;

    /**
     * The temp directory for installing flutter.
     */
    @Parameter(property = "flutter-temp-directory", defaultValue = "${basedir}/target/temp", required = false)
    protected File tempDirectory;

    /**
     * Additional environment variables to pass to the build.
     */
    @Parameter
    protected Map<String, String> environmentVariables;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    /**
     * Determines if this execution should be skipped.
     */
    private boolean skipTestPhase() {
        return skipTests && isTestingPhase();
    }

    /**
     * Determines if the current execution is during a testing phase (e.g., "test" or "integration-test").
     */
    private boolean isTestingPhase() {
        String phase = execution.getLifecyclePhase();
        return "test".equals(phase) || "integration-test".equals(phase);
    }

    protected abstract void execute(FlutterPluginFactory factory) throws FlutterException;

    /**
     * Implemented by children to determine if this execution should be skipped.
     */
    protected abstract boolean skipExecution();

    @Override
    public void execute() throws MojoFailureException {
        if (testFailureIgnore && !isTestingPhase()) {
            getLog().info("testFailureIgnore property is ignored in non test phases");
        }
        if (!(skipTestPhase() || skipExecution())) {
            if (installDirectory == null) {
                installDirectory = workingDirectory;
            }
            try {
                execute(new FlutterPluginFactory(workingDirectory, installDirectory, tempDirectory,
                        new RepositoryCacheResolver(repositorySystemSession)));
            } catch (TaskRunnerException e) {
                if (testFailureIgnore && isTestingPhase()) {
                    getLog().error("There are test failures.\nFailed to run task: " + e.getMessage(), e);
                } else {
                    throw new MojoFailureException("Failed to run task", e);
                }
            } catch (FlutterException e) {
                throw MojoUtils.toMojoFailureException(e);
            }
        } else {
            getLog().info("Skipping execution.");
        }
    }

}
