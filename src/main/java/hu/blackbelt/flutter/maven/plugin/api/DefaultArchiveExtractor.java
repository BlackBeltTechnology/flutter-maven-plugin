package hu.blackbelt.flutter.maven.plugin.api;

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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DefaultArchiveExtractor implements ArchiveExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultArchiveExtractor.class);

    private void prepDestination(File path, boolean directory) throws IOException {
        if (directory) {
            path.mkdirs();
        } else {
            if (!path.getParentFile().exists()) {
                path.getParentFile().mkdirs();
            }
            if (!path.getParentFile().canWrite()) {
                throw new AccessDeniedException(
                        String.format("Could not get write permissions for '%s'", path.getParentFile().getAbsolutePath()));
            }
        }
    }

    @Override
    public void extract(String archive, String destinationDirectory) throws ArchiveExtractionException {
        final File archiveFile = new File(archive);

        try (FileInputStream fis = new FileInputStream(archiveFile)) {
            if ("msi".equals(FileUtils.getExtension(archiveFile.getAbsolutePath()))) {
                String command = "msiexec /a " + archiveFile.getAbsolutePath() + " /qn TARGETDIR=\""
                        + destinationDirectory + "\"";
                Process child = Runtime.getRuntime().exec(command);
                try {
                    int result = child.waitFor();
                    if (result != 0) {
                        throw new ArchiveExtractionException(
                                "Could not extract " + archiveFile.getAbsolutePath() + "; return code " + result);
                    }
                } catch (InterruptedException e) {
                    throw new ArchiveExtractionException(
                            "Unexpected interruption of while waiting for extraction process", e);
                }
            } else if ("zip".equals(FileUtils.getExtension(archiveFile.getAbsolutePath()))) {
                ZipFile zipFile = new ZipFile(archiveFile);
                try {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        final File destPath = new File(destinationDirectory + File.separator + entry.getName());
                        prepDestination(destPath, entry.isDirectory());
                        if (!entry.isDirectory()) {
                            InputStream in = null;
                            OutputStream out = null;
                            try {
                                in = zipFile.getInputStream(entry);
                                out = new FileOutputStream(destPath);
                                IOUtils.copy(in, out);
                            } finally {
                                IOUtils.closeQuietly(in);
                                IOUtils.closeQuietly(out);
                            }
                        }
                    }
                } finally {
                    zipFile.close();
                }
            } else if ("xz".equals(FileUtils.getExtension(archiveFile.getAbsolutePath()))) {
                // TarArchiveInputStream can be constructed with a normal FileInputStream if
                // we ever need to extract regular '.tar' files.
                TarArchiveInputStream tarIn = null;
                try {
                    tarIn = new TarArchiveInputStream(new XZCompressorInputStream(fis));

                    TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
                    String canonicalDestinationDirectory = new File(destinationDirectory).getCanonicalPath();
                    while (tarEntry != null) {
                        // Create a file for this tarEntry
                        final File destPath = new File(destinationDirectory + File.separator + tarEntry.getName());
                        prepDestination(destPath, tarEntry.isDirectory());

                        if (!startsWithPath(destPath.getCanonicalPath(), canonicalDestinationDirectory)) {
                            throw new IOException(
                                    "Expanding " + tarEntry.getName() + " would create file outside of " + canonicalDestinationDirectory
                            );
                        }

                        if (!tarEntry.isDirectory()) {
                            destPath.createNewFile();
                            boolean isExecutable = (tarEntry.getMode() & 0100) > 0;
                            destPath.setExecutable(isExecutable);

                            OutputStream out = null;
                            try {
                                out = new FileOutputStream(destPath);
                                IOUtils.copy(tarIn, out);
                            } finally {
                                IOUtils.closeQuietly(out);
                            }
                        }
                        tarEntry = tarIn.getNextTarEntry();
                    }
                } finally {
                    IOUtils.closeQuietly(tarIn);
                }
            } else {
                // TarArchiveInputStream can be constructed with a normal FileInputStream if
                // we ever need to extract regular '.tar' files.
                TarArchiveInputStream tarIn = null;
                try {
                    tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(fis));

                    TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
                    String canonicalDestinationDirectory = new File(destinationDirectory).getCanonicalPath();
                    while (tarEntry != null) {
                        // Create a file for this tarEntry
                        final File destPath = new File(destinationDirectory + File.separator + tarEntry.getName());
                        prepDestination(destPath, tarEntry.isDirectory());

                        if (!startsWithPath(destPath.getCanonicalPath(), canonicalDestinationDirectory)) {
                            throw new IOException(
                                    "Expanding " + tarEntry.getName() + " would create file outside of " + canonicalDestinationDirectory
                            );
                        }

                        if (!tarEntry.isDirectory()) {
                            destPath.createNewFile();
                            boolean isExecutable = (tarEntry.getMode() & 0100) > 0;
                            destPath.setExecutable(isExecutable);

                            OutputStream out = null;
                            try {
                                out = new FileOutputStream(destPath);
                                IOUtils.copy(tarIn, out);
                            } finally {
                                IOUtils.closeQuietly(out);
                            }
                        }
                        tarEntry = tarIn.getNextTarEntry();
                    }
                } finally {
                    IOUtils.closeQuietly(tarIn);
                }
            }
        } catch (IOException e) {
            throw new ArchiveExtractionException("Could not extract archive: '"
                    + archive
                    + "'", e);
        }
    }

    /**
     * Do multiple file system checks that should enable the plugin to work on any file system
     * whether or not it's case sensitive or not.
     *
     * @param destPath
     * @param destDir
     * @return
     */
    private boolean startsWithPath(String destPath, String destDir) {
        if (destPath.startsWith(destDir)) {
            return true;
        } else if (destDir.length() > destPath.length()) {
            return false;
        } else {
            if (new File(destPath).exists() && !(new File(destPath.toLowerCase()).exists())) {
                return false;
            }

            return destPath.toLowerCase().startsWith(destDir.toLowerCase());
        }
    }
}
