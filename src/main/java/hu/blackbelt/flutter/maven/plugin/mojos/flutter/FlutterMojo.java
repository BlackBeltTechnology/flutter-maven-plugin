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

import hu.blackbelt.flutter.maven.plugin.api.MojoUtils;
import hu.blackbelt.flutter.maven.plugin.api.ProxyConfig;
import hu.blackbelt.flutter.maven.plugin.api.TaskRunnerException;
import hu.blackbelt.flutter.maven.plugin.flutter.FlutterPluginFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Collections;

@Mojo(name="flutter", defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        threadSafe = true,
        instantiationStrategy = InstantiationStrategy.PER_LOOKUP,
        executionStrategy = "always" )
public final class FlutterMojo extends AbstractFlutterMojo {

    /**
     * flutter arguments. Default is "pub get".
     */
    @Parameter(defaultValue = "pub get", property = "flutter.arguments", required = false)
    private String arguments;


    @Parameter(property = "flutter.flutterInheritsProxyConfigFromMaven", required = false, defaultValue = "true")
    private boolean flutterInheritsProxyConfigFromMaven;

    /**
     * Server Id for download username and password
     */
    @Parameter(property = "serverId", defaultValue = "")
    private String serverId;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private BuildContext buildContext;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "flutter-skip", defaultValue = "${flutter-skip}")
    private boolean skip;

    /**
     * Parallel enabled. Default is "false".
     */
    @Parameter(defaultValue = "false", property = "flutter.parallel", required = false)
    private Boolean parallel;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    private ProxyConfig getProxyConfig() {
        if (flutterInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(session, decrypter);
        } else {
            getLog().info("flutter not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.<ProxyConfig.Proxy>emptyList());
        }
    }

    @Override
    public void execute(FlutterPluginFactory factory) throws TaskRunnerException {
        if (!parallel) {
            synchronized (lock) {
                executeTask(factory);
            }
        } else {
            executeTask(factory);
        }
    }

    private void executeTask(FlutterPluginFactory factory) throws TaskRunnerException {
        File pubspec = new File(workingDirectory, "pubspec.yaml");
        if (buildContext == null || buildContext.hasDelta(pubspec) || !buildContext.isIncremental()) {
            ProxyConfig proxyConfig = getProxyConfig();
            factory.getFlutterExecutor(proxyConfig).execute(arguments, environmentVariables);
        } else {
            getLog().info("Skipping flutter pub get as pubspec.yaml unchanged");
        }
    }
}
