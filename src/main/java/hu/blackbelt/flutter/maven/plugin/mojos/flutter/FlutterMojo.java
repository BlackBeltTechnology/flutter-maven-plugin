package hu.blackbelt.flutter.maven.plugin.mojos.flutter;

import hu.blackbelt.flutter.maven.plugin.api.MojoUtils;
import hu.blackbelt.flutter.maven.plugin.api.ProxyConfig;
import hu.blackbelt.flutter.maven.plugin.api.TaskRunnerException;
import hu.blackbelt.flutter.maven.plugin.flutter.FlutterPluginFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Collections;

@Mojo(name="flutter", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
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
        File pubspec = new File(workingDirectory, "pubspec.yaml");
        if (buildContext == null || buildContext.hasDelta(pubspec) || !buildContext.isIncremental()) {
            ProxyConfig proxyConfig = getProxyConfig();
            factory.getFlutterExecutor(proxyConfig).execute(arguments, environmentVariables);
        } else {
            getLog().info("Skipping flutter pub get as pubspec.yaml unchanged");
        }
    }
}
