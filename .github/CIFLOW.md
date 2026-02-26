# Development Version and Branch Handling

This document describes the GitFlow-based branching strategy and CI/CD pipeline used by flutter-maven-plugin. All version management and automated workflows follow these conventions.

## Branches

The branching model is based on [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow). Each branch type has a specific role in the development lifecycle:

```mermaid
gitGraph
    commit id: "initial"
    branch develop
    checkout develop
    commit id: "dev-1"
    branch feature/JNG-1
    commit id: "feat-1a"
    commit id: "feat-1b"
    checkout develop
    merge feature/JNG-1 id: "merge-feat-1"
    branch feature/JNG-2
    commit id: "feat-2a"
    checkout develop
    merge feature/JNG-2 id: "merge-feat-2"
    branch release/1.0-beta1
    commit id: "rc-1"
    branch bugfix/JNG-4
    commit id: "fix-4"
    checkout release/1.0-beta1
    merge bugfix/JNG-4 id: "merge-fix"
    checkout develop
    merge release/1.0-beta1 id: "merge-release"
    checkout main
    merge release/1.0-beta1 id: "release-1.0"
```

| Branch Pattern | Base | Purpose |
|----------------|------|---------|
| `develop` | — | Main development branch containing latest sources of the active version |
| `feature/JNG-NUMBER_summary` | `develop` | New features for the next release |
| `release/X.Y-betaN` | `develop` | Release candidates; the `release/` prefix is reserved for CI |
| `bugfix/JNG-NUMBER_summary` | `release/*` | Bug fixes on release branches; must also be applied to newer release and develop branches |
| `support/JNG-NUMBER_summary` | `release/*` | Support for previous releases with minor changes; merged back to the release branch |
| `master` | — | Contains the latest released sources |
| `hotfix/JNG-NUMBER_summary` | `master` | Critical fixes applied to both release and master branches |

## Version Numbers

Version numbers follow semantic versioning with these rules:

| Event | Version Change |
|-------|---------------|
| Start a `feature/` branch | No change |
| Start a `release/` branch | 2nd number increases on `develop` |
| Start a `bugfix/` branch | No change (applied on release branches before merge to master) |
| Start a `support/` branch | 3rd number increases |
| Start a `hotfix/` branch | 4th number increases |

## GitHub Actions Workflows

The CI/CD pipeline is composed of four interconnected workflows that automate building, testing, versioning, merging, and releasing.

### build.yml — Main Build Pipeline

This workflow triggers on pushes to `develop` and pull requests targeting `develop`, `master`, `increment/*`, or `release/*` branches.

```mermaid
flowchart TD
    A[Push on develop or PR on develop/master/increment/release] --> B{Branch type?}
    B -->|master, release/*| C[Set version from pom.xml\nwithout -SNAPSHOT]
    B -->|develop, increment/*| D[Set version as\nmajor.minor.qualifier.date_commitId_branch]
    C --> E[Build and deploy to Nexus]
    D --> E
    E --> F[Create git tag v-version]
    F --> G{Branch type?}
    G -->|increment/*, release/*| H[Create merge-pr/version tag]
    H --> I[Trigger merge-pr-tagged.yml]
    G -->|develop| J[Build changelog]
    J --> K[Create GitHub prerelease]
    G -->|other| L[Done]
```

### merge-pr-tagged.yml — Pull Request Merge Automation

Triggered when a `merge-pr/*` tag is pushed. Handles automatic merging of pull requests based on version format.

```mermaid
flowchart TD
    A[Push on merge-pr/* tag] --> B[Extract version from tag]
    B --> C{Version format?}
    C -->|major.minor.qualifier| D[Merge PR to master]
    D --> E[Trigger create-release-on-master.yml]
    C -->|other format| F[Squash PR to develop]
    F --> G[Trigger build.yml]
    C --> H[Delete merge-pr/version tag]
```

### create-release-on-master.yml — Release Creation

Triggered by pushes to `master`. Creates the official GitHub release with a changelog.

```mermaid
flowchart TD
    A[Push on master] --> B[Get version from tag]
    B --> C[Build changelog]
    C --> D[Create GitHub release as latest]
```

### release.yml — Release Initiation

Manually triggered with a version parameter. Creates the pull requests that start the release process.

```mermaid
flowchart TD
    A[Manual trigger with version] --> B{Version = 'auto'?}
    B -->|Yes| C[Use version from pom.xml\nwithout -SNAPSHOT]
    B -->|No| D[Use given version]
    C --> E[Set next version = qualifier + 1]
    D --> E
    E --> F[Create PR on master\nwith release version]
    F --> G[Trigger build.yml]
    E --> H[Create PR on develop\nwith next version]
    H --> I[Trigger build.yml]
```

## How to Develop

Issue tracking uses [JIRA](https://blackbelt.atlassian.net/jira/dashboards).

> **Important:** There is no commit without a ticket number. Every pull request and commit must reference a `JNG-xxx` ticket.
