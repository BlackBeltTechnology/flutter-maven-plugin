package hu.blackbelt.flutter.maven.plugin.api;

public interface FileDownloader {
    void download(String downloadUrl, String destination, String userName, String password) throws DownloadException;
}

