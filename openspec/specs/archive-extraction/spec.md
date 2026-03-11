# Archive Extraction Specification

## Purpose

The archive-extraction capability handles extracting Flutter SDK archives in multiple formats (ZIP, TAR.XZ, TAR.GZ, MSI) with path traversal protection and proper file permission preservation.

## Architecture

The `ArchiveExtractor` interface defines a single `extract(archive, destination)` method. `DefaultArchiveExtractor` implements it with format detection based on file extension, using Apache Commons Compress for TAR/XZ/GZIP and Java's built-in `ZipFile` for ZIP archives.

Key classes:
- `ArchiveExtractor` — Interface defining the extraction contract
- `DefaultArchiveExtractor` — Implementation supporting ZIP, TAR.XZ, TAR.GZ, and MSI formats

## Requirements

### Requirement: Multi-format archive support

The extractor SHALL support ZIP, TAR.XZ, TAR.GZ, and MSI archive formats based on file extension.

#### Scenario: Extract ZIP archive (Windows/macOS Flutter)
- **GIVEN** an archive file with `.zip` extension
- **WHEN** `extract()` is called
- **THEN** all entries are extracted to the destination directory
- **AND** directory structure is preserved

#### Scenario: Extract TAR.XZ archive (Linux Flutter)
- **GIVEN** an archive file with `.xz` extension
- **WHEN** `extract()` is called
- **THEN** the XZ-compressed TAR archive is decompressed and extracted
- **AND** executable permissions from TAR entries (mode bit `0100`) are preserved on extracted files

#### Scenario: Extract TAR.GZ archive
- **GIVEN** an archive file with `.gz` or `.tar.gz` extension
- **WHEN** `extract()` is called
- **THEN** the GZIP-compressed TAR archive is decompressed and extracted

#### Scenario: Extract MSI installer (Windows)
- **GIVEN** an archive file with `.msi` extension
- **WHEN** `extract()` is called
- **THEN** `msiexec /a` is invoked with the archive path and destination directory
- **AND** a non-zero return code throws `ArchiveExtractionException`

### Requirement: Path traversal protection

The extractor SHALL prevent zip-slip attacks by validating that extracted file paths remain within the destination directory.

#### Scenario: Malicious archive with path traversal entry
- **GIVEN** a TAR archive contains an entry with path `../../etc/passwd`
- **WHEN** the entry is processed during extraction
- **THEN** an `IOException` is thrown indicating the entry would escape the destination directory
- **AND** no file is written outside the destination

### Requirement: Directory preparation

The extractor SHALL create parent directories as needed during extraction.

#### Scenario: Nested directory structure
- **GIVEN** an archive entry has path `flutter/bin/cache/dart-sdk/bin/dart`
- **WHEN** the entry is extracted
- **THEN** all intermediate directories are created if they don't exist
- **AND** write permissions are verified on parent directories

#### Scenario: Write permission denied
- **GIVEN** the parent directory of an entry is not writable
- **WHEN** the entry is extracted
- **THEN** an `AccessDeniedException` is thrown with the directory path
