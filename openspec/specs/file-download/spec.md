# File Download Specification

## Purpose

The file-download capability handles downloading Flutter SDK archives over HTTP/HTTPS with proxy support, authentication, and local file URI support.

## Architecture

The `FileDownloader` interface defines a single `download(url, destination, username, password)` method. `DefaultFileDownloader` implements it using Apache HttpClient 4.x with configurable proxy settings and credential providers.

Key classes:
- `FileDownloader` — Interface defining the download contract
- `DefaultFileDownloader` — Implementation with proxy, auth, and file URI support
- `ProxyConfig` — Provides proxy configuration for download routing

## Requirements

### Requirement: HTTP/HTTPS file download

The downloader SHALL download files from HTTP/HTTPS URLs to a local destination path.

#### Scenario: Successful download
- **GIVEN** a valid HTTPS URL pointing to a Flutter archive
- **WHEN** `download()` is called
- **THEN** the file is downloaded using TLS 1.2
- **AND** saved to the specified destination path
- **AND** parent directories are created if missing

#### Scenario: Server returns error
- **GIVEN** the server responds with a non-200 status code
- **WHEN** the download is attempted
- **THEN** a `DownloadException` is thrown with the status code

### Requirement: Local file URI support

The downloader SHALL support `file://` URIs by copying the local file.

#### Scenario: File URI download
- **GIVEN** a URL with `file` scheme (e.g., `file:///path/to/flutter.tar.xz`)
- **WHEN** `download()` is called
- **THEN** the file is copied locally using `FileUtils.copyFile()`

### Requirement: Proxy-aware downloads

The downloader SHALL route requests through configured proxies when available.

#### Scenario: Download through proxy
- **GIVEN** a proxy is configured in `ProxyConfig` that matches the download URL
- **WHEN** the download is attempted
- **THEN** the HTTP request is routed through the proxy host and port

#### Scenario: Authenticated proxy
- **GIVEN** the proxy requires authentication
- **WHEN** the download is routed through the proxy
- **THEN** proxy credentials are provided via `CredentialsProvider`

#### Scenario: No matching proxy
- **GIVEN** no proxy matches the download URL (or the URL's host is in the non-proxy list)
- **WHEN** the download is attempted
- **THEN** a direct connection is used

### Requirement: Server credential authentication

The downloader SHALL support Basic authentication using credentials from Maven `settings.xml`.

#### Scenario: Download with server credentials
- **GIVEN** `username` and `password` are provided (non-empty)
- **AND** no proxy is configured
- **WHEN** the download is attempted
- **THEN** Basic auth credentials are sent preemptively to the target host
- **AND** an `AuthCache` with `BasicScheme` is configured in the HTTP context

### Requirement: TLS 1.2 enforcement

The downloader SHALL enforce TLS 1.2 as the minimum protocol version.

#### Scenario: HTTPS download
- **WHEN** any download is initiated
- **THEN** the system property `https.protocols` is set to `TLSv1.2`
