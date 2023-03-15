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
import hu.blackbelt.flutter.maven.plugin.api.InstallationException;
import hu.blackbelt.flutter.maven.plugin.api.ProxyConfig;
import hu.blackbelt.flutter.maven.plugin.flutter.FlutterPluginFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.Server;

@Mojo(name="install-flutter", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallFlutterMojo extends AbstractFlutterMojo {

    /**
     * Where to download Flutter binary from. Defaults to https://storage.googleapis.com/flutter_infra/releases/
     */
    @Parameter(property = "flutter-download-root", required = false, defaultValue = "https://storage.googleapis.com/flutter_infra_release/releases/")
    private String flutterDownloadRoot;

    /**
     * Where to download Flutter git from. Defaults to https://github.com/flutter/flutter.git
     */
    @Parameter(property = "flutter-git-url", required = false, defaultValue = "https://github.com/flutter/flutter.git")
    private String flutterGitUrl;

    /**
     * The version of Flutter to install.
     */
    @Parameter(property="flutter-version", required = false)
    private String flutterVersion;

    /**
     * The channel of Flutter to install.
     */
    @Parameter(property="flutter-channel", defaultValue = "stable", required = false)
    private String flutterChannel;

    /**
     * Server Id for download username and password
     */
    @Parameter(property = "serverId", defaultValue = "")
    private String serverId;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "flutter-install-skip", defaultValue = "${flutter-install-skip}")
    private boolean skip;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FlutterPluginFactory factory) throws InstallationException {
        synchronized(lock) {

            ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session, decrypter);
            String flutterDownloadRoot = getFlutterDownloadRoot();
            String flutterGitUrl = getFlutterGitUrl();
            Server server = MojoUtils.decryptServer(serverId, session, decrypter);
            if (null != server) {
                factory.getFlutterInstaller(proxyConfig)
                        .setFlutterVersion(flutterVersion)
                        .setFlutterChannel(flutterChannel)
                        .setFlutterDownloadRoot(flutterDownloadRoot)
                        .setFlutterGitUrl(flutterGitUrl)
                        .setUserName(server.getUsername())
                        .setPassword(server.getPassword())
                        .install();
            } else {
                factory.getFlutterInstaller(proxyConfig)
                        .setFlutterVersion(flutterVersion)
                        .setFlutterChannel(flutterChannel)
                        .setFlutterDownloadRoot(flutterDownloadRoot)
                        .setFlutterGitUrl(flutterGitUrl)
                        .install();
            }
        }
    }

    private String getFlutterDownloadRoot() {
        return flutterDownloadRoot;
    }

    private String getFlutterGitUrl() {
        return flutterGitUrl;
    }

}
