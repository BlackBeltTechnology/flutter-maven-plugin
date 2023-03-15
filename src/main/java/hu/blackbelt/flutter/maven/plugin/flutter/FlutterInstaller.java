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
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlutterInstaller {

    public static final Pattern FLUTTER_VERSION_INFO = Pattern.compile("Flutter.(.*).•.channel.(.*).•(.*)");

    private static final Object LOCK = new Object();

    private String flutterVersion, flutterChannel, flutterDownloadRoot, flutterGitUrl, userName, password;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    private final ProxyConfig proxyConfig;

    FlutterInstaller(InstallConfig config, ProxyConfig proxyConfig, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
        this.proxyConfig = proxyConfig;
    }

    public FlutterInstaller setFlutterVersion(String flutterVersion) {
        this.flutterVersion = flutterVersion;
        return this;
    }

    public FlutterInstaller setFlutterChannel(String flutterChannel) {
        this.flutterChannel = flutterChannel;
        return this;
    }

    public FlutterInstaller setFlutterDownloadRoot(String flutterDownloadRoot) {
        this.flutterDownloadRoot = flutterDownloadRoot;
        return this;
    }

    public FlutterInstaller setFlutterGitUrl(String flutterGitUrl) {
        this.flutterGitUrl = flutterGitUrl;
        return this;
    }

    public FlutterInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public FlutterInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (this.flutterDownloadRoot == null || this.flutterDownloadRoot.isEmpty()) {
                this.flutterDownloadRoot = this.config.getPlatform().getFlutterDownloadRoot();
            }
            if (this.flutterGitUrl == null || this.flutterGitUrl.isEmpty()) {
                this.flutterGitUrl = this.config.getPlatform().getFlutterGitUrl();
            }

            if (!flutterIsAlreadyInstalled()) {
                if (this.flutterVersion != null) {
                    this.logger.info("Installing Flutter version {} from archive", this.flutterVersion);
                    if (this.config.getPlatform().isWindows()) {
                        installFlutterForWindowsFromArchive();
                    } else {
                        installFlutterDefaultFromArchive();
                    }
                } else {
                    this.logger.info("Installing Flutter channel {} from git", this.flutterChannel);
                    installFlutterDefaultFromGit();
                }
            }
        }
    }

    private boolean flutterIsAlreadyInstalled() {
        try {
            FlutterExecutorConfig executorConfig = new DefaultFlutterExecutorConfig(this.config);
            File flutterFile = executorConfig.getFlutterPath();
            try {
                setExecutablePermission();
            } catch (InstallationException | IOException e) {
                logger.error("Could not set permissions");
            }

            if (flutterFile.exists()) {
                final String version =
                    new FlutterExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult(logger);

                // this.logger.info("Flutter info: {}", version);

                Matcher matcher = FLUTTER_VERSION_INFO.matcher(version);
                if (matcher.find()) {
                    String installedVersion = matcher.group(1);
                    String installedChannel = matcher.group(2);

                    this.logger.info("Flutter {} is already installed on channel: {}. ", installedVersion, installedChannel);

                    if (flutterVersion != null) {
                        if (this.flutterVersion.equals(installedVersion)) {
                            return true;
                        } else {
                            this.logger.info("Requested version {} is not matched, switching to: {}", installedVersion, this.flutterVersion);
                            return false;
                        }
                    } else {
                        if (flutterChannel == null || !this.flutterChannel.equals(installedChannel)) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                } else {
                    this.logger.warn("Could not determinate version");
                    return false;
                }
            } else {
                // this.logger.info("Flutter does not exists");
                return false;
            }
        } catch (ProcessExecutionException e) {
            this.logger.warn("Could not exec", e);
            return false;
        }
    }

    private void installFlutterForWindowsFromArchive() throws InstallationException {
        try {
            String downloadUrl = this.flutterDownloadRoot
                    + this.config.getPlatform().getFlutterDownloadFilename(this.flutterVersion);

            File tmpDirectory = getTempDirectory();

            CacheDescriptor cacheDescriptor = new CacheDescriptor("flutter", this.flutterVersion,
                    this.config.getPlatform().getArchiveExtension());

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            try {
                extractFile(archive, tmpDirectory);
            } catch (ArchiveExtractionException e) {
                if (e.getCause() instanceof EOFException) {
                    // https://github.com/eirslett/frontend-maven-plugin/issues/794
                    // The downloading was probably interrupted and archive file is incomplete:
                    // delete it to retry from scratch
                    this.logger.error("The archive file {} is corrupted and will be deleted. "
                            + "Please try the build again.", archive.getPath());
                    archive.delete();
                    FileUtils.deleteDirectory(tmpDirectory);
                }
                throw e;
            }
            // Search for the flutter binary
            File flutterBinary = new File(tmpDirectory, "flutter" + File.separator + "bin" + File.separator + "flutter.bat");
            if (!flutterBinary.exists()) {
                throw new FileNotFoundException(
                        "Could not find the downloaded Flutter binary in " + flutterBinary);
            } else {
                File destinationDirectory = getInstallDirectory();
                deleteInstallDirectory(getInstallDirectory());
                File extractedDirectory = new File(tmpDirectory, "flutter");
                this.logger.info("Moving Flutter directory from {} to {}", extractedDirectory, destinationDirectory);
                try {
                    Files.move(extractedDirectory.toPath(), destinationDirectory.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new InstallationException("Could not install Flutter: Was not allowed to rename "
                            + extractedDirectory + " to " + destinationDirectory);
                }

                deleteTempDirectory(tmpDirectory);

                FlutterExecutorConfig executorConfig = new DefaultFlutterExecutorConfig(this.config);
                new FlutterExecutor(executorConfig, Arrays.asList("doctor"), null).executeAndRedirectOutput(logger);

                if (!flutterIsAlreadyInstalled()) {
                    throw new InstallationException("Could not install Flutter: Was not able to execute version check");
                }

                this.logger.info("Installed flutter locally.");
            }
        } catch (IOException e) {
            throw new InstallationException("Could not install Flutter", e);
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Flutter", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the Flutter archive", e);
        } catch (ProcessExecutionException e) {
            throw new InstallationException("Could not execute Flutter doctor", e);
        }

    }

    private void installFlutterDefaultFromArchive() throws InstallationException {
        try {
            String downloadUrl = this.flutterDownloadRoot
                + this.config.getPlatform().getFlutterDownloadFilename(this.flutterVersion);

            File tmpDirectory = getTempDirectory();

            CacheDescriptor cacheDescriptor = new CacheDescriptor("flutter", this.flutterVersion,
                this.config.getPlatform().getArchiveExtension());

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            try {
                extractFile(archive, tmpDirectory);
            } catch (ArchiveExtractionException e) {
                if (e.getCause() instanceof EOFException) {
                    // https://github.com/eirslett/frontend-maven-plugin/issues/794
                    // The downloading was probably interrupted and archive file is incomplete:
                    // delete it to retry from scratch
                    this.logger.error("The archive file {} is corrupted and will be deleted. "
                            + "Please try the build again.", archive.getPath());
                    archive.delete();
                    FileUtils.deleteDirectory(tmpDirectory);
                }

                throw e;
            }

            // Search for the flutter binary
            File flutterBinary =
                new File(tmpDirectory, "flutter" + File.separator + "bin" + File.separator + "flutter");
            if (!flutterBinary.exists()) {
                throw new FileNotFoundException(
                    "Could not find the downloaded Flutter binary in " + flutterBinary);
            } else {
                File destinationDirectory = getInstallDirectory();
                deleteInstallDirectory(getInstallDirectory());
                File extractedDirectory = new File(tmpDirectory, "flutter");
                this.logger.info("Moving Flutter directory from {} to {}", extractedDirectory, destinationDirectory);
                try {
                    Files.move(extractedDirectory.toPath(), destinationDirectory.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new InstallationException("Could not install Flutter: Was not allowed to rename "
                            + extractedDirectory + " to " + destinationDirectory);
                }
                setExecutablePermission();

                deleteTempDirectory(tmpDirectory);

                FlutterExecutorConfig executorConfig = new DefaultFlutterExecutorConfig(this.config);
                new FlutterExecutor(executorConfig, Arrays.asList("doctor"), null).executeAndRedirectOutput(logger);

                if (!flutterIsAlreadyInstalled()) {
                    throw new InstallationException("Could not install Flutter: Was not able to execute version check");
                }

                this.logger.info("Installed Flutter locally.");
            }
        } catch (IOException e) {
            throw new InstallationException("Could not install Flutter", e);
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Flutter", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the Flutter archive", e);
        } catch (ProcessExecutionException e) {
            throw new InstallationException("Could not execute Flutter doctor", e);
        }
    }

    private void setExecutablePermission() throws InstallationException, IOException {
        File destinationDirectory = getInstallDirectory();

        addExecPermission(new File(destinationDirectory, "bin" + File.separator + "flutter"));
        addExecPermission(new File(destinationDirectory, "bin" + File.separator + "dart"));
        addExecPermission(new File(destinationDirectory, "bin" + File.separator + "internal" + File.separator + "shared.sh"));
        addExecPermission(new File(destinationDirectory, "bin" + File.separator + "internal" + File.separator + "update_dart_sdk.sh"));
        addExecPermission(new File(destinationDirectory, "bin" + File.separator + "cache" + File.separator + "dart-sdk" + File.separator + "bin"));
    }

    private void installFlutterDefaultFromGit() throws InstallationException {
        try {
            setupGitProxy();
            File destinationDirectory = getInstallDirectory();

            File gitDir = new File(destinationDirectory, ".git");
            Git git;
            if (gitDir.exists()) {
                this.logger.info("Using existing Flutter git. Set remote to: " + flutterGitUrl);
                git = Git.open(destinationDirectory);
                if (!git.status().call().isClean()) {
                    this.logger.info("Stashing dirty state");
                    git.stashCreate().call();
                }
            } else {
                this.logger.info("Cloning Flutter from git");
                git = Git.cloneRepository()
                        .setURI(this.flutterGitUrl)
                        .setDirectory(destinationDirectory)
                        .call();
            }

            FlutterExecutorConfig executorConfig = new DefaultFlutterExecutorConfig(this.config);

            this.logger.info("Set Flutter to channel:"  + flutterChannel);
            new FlutterExecutor(executorConfig, Arrays.asList("channel", flutterChannel), null).executeAndRedirectOutput(logger);
            this.logger.info("Flutter upgrade");
            new FlutterExecutor(executorConfig, Arrays.asList("upgrade"), null).executeAndRedirectOutput(logger);

            if (!flutterIsAlreadyInstalled()) {
                throw new InstallationException("Could not install Flutter: Was not able to execute version check");
            }

        } catch (InvalidRemoteException e) {
            throw new InstallationException("Could not install Flutter", e);
        } catch (TransportException e) {
            throw new InstallationException("Could not install Flutter", e);
        } catch (GitAPIException e) {
            throw new InstallationException("Could not install Flutter", e);
        } catch (IOException e) {
            throw new InstallationException("Could not install Flutter", e);
        } catch (ProcessExecutionException e) {
            throw new InstallationException("Could not execute Flutter", e);
        }
    }

    private void setExecutable(File destinationBinary) throws InstallationException {
        if (!destinationBinary.exists()) {
            return;
        }
        if (!destinationBinary.setExecutable(true, false)) {
            throw new InstallationException(
                    "Could not install Flutter: Was not allowed to make " + destinationBinary + " executable.");
        }

    }

    private File getTempDirectory() {
        File tmpDirectory = this.config.getTempDirectory(); //new File(getInstallDirectory(), "tmp");
        if (!tmpDirectory.exists()) {
            this.logger.debug("Creating temporary directory {}", tmpDirectory);
            tmpDirectory.mkdirs();
        }
        return tmpDirectory;
    }

    private File getInstallDirectory() {
        File installDirectory = this.config.getInstallDirectory(); // new File(this.config.getInstallDirectory(), INSTALL_PATH);
        return installDirectory;
    }

    private void deleteTempDirectory(File tmpDirectory) throws IOException {
        if (tmpDirectory != null && tmpDirectory.exists()) {
            this.logger.debug("Deleting temporary directory {}", tmpDirectory);
            FileUtils.deleteDirectory(tmpDirectory);
        }
    }

    private void deleteInstallDirectory(File installDirectory) throws IOException {
        if (installDirectory != null && installDirectory.exists()) {
            this.logger.debug("Deleting install directory {}", installDirectory);
            FileUtils.deleteDirectory(installDirectory);
        }
    }

    private void extractFile(File archive, File destinationDirectory) throws ArchiveExtractionException {
        this.logger.info("Unpacking {} into {}", archive, destinationDirectory);
        this.archiveExtractor.extract(archive.getPath(), destinationDirectory.getPath());
    }

    private void downloadFileIfMissing(String downloadUrl, File destination, String userName, String password)
        throws DownloadException {
        if (!destination.exists()) {
            downloadFile(downloadUrl, destination, userName, password);
        }
    }

    private void downloadFile(String downloadUrl, File destination, String userName, String password)
        throws DownloadException {
        this.logger.info("Downloading {} to {}", downloadUrl, destination);
        this.fileDownloader.download(downloadUrl, destination.getPath(), userName, password);
    }

    private void setupGitProxy() {

        /*
        Authenticator.setDefault(new Authenticator() {
            final Authenticator delegate = Authenticator.
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                // If proxy is non authenticated for some URLs, the requested URL is the endpoint (and not the proxy host)
                // In this case the authentication should not be the one of proxy ... so return null (and JGit CredentialsProvider will be used)
                if (super.getRequestingHost().equals("localhost")) {
                    return new PasswordAuthentication("foo", "bar".toCharArray());
                }
                return null;
            }
        }); */

        ProxySelector.setDefault(new ProxySelector() {
            final ProxySelector delegate = ProxySelector.getDefault();

            @Override
            public List<Proxy> select(URI uri) {
                // Filter the URIs to be proxied
                if (uri.toString().equals(flutterGitUrl)) {
                    ProxyConfig.Proxy proxy = proxyConfig.getProxyForUrl(uri.toString());
                    return Arrays.asList(new Proxy(Proxy.Type.HTTP,
                                InetSocketAddress.createUnresolved(proxy.host, proxy.port)));
                }
                // revert to the default behaviour
                return delegate == null ? Arrays.asList(Proxy.NO_PROXY)
                        : delegate.select(uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                if (uri == null || sa == null || ioe == null) {
                    throw new IllegalArgumentException(
                            "Arguments can't be null.");
                }
            }
        });
    }

    private void addExecPermission(final File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            Files.walk(file.toPath()).filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)).forEach(f -> {
                try {
                    addExecPermission(f.toFile());
                } catch (IOException e) {
                }
            });
        }
        Path path = file.toPath();

        Set<String> fileAttributeView = FileSystems.getDefault().supportedFileAttributeViews();

        if (fileAttributeView.contains("posix")) {
            final Set<PosixFilePermission> permissions;
            try {
                permissions = Files.getPosixFilePermissions(path);
            } catch (UnsupportedOperationException e) {
                logger.debug("Exec file permission is not set", e);
                return;
            }
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);

        } else if (fileAttributeView.contains("acl")) {
            String username = System.getProperty("user.name");
            UserPrincipal userPrincipal = FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(username);
            AclEntry aclEntry = AclEntry.newBuilder().setPermissions(AclEntryPermission.EXECUTE).setType(AclEntryType.ALLOW).setPrincipal(userPrincipal).build();

            AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            List<AclEntry> aclEntries = acl.getAcl();
            aclEntries.add(aclEntry);
            acl.setAcl(aclEntries);
        }
    }

}
