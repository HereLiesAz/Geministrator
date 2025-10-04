# Jules IDE Agent Guide

This document provides guidance for AI agents working on the Jules IDE project.

## Project Overview

This project is a mobile Integrated Development Environment (IDE) that acts as a client for the Jules API. It allows users to connect to their source code repositories, interact with the Jules agent, and manage their software development lifecycle from an Android device.

## Architecture

The project is a multi-module Gradle project with the following structure:

-   **:app_android**: The main Android application, built with Jetpack Compose. It contains all the UI and application logic.
-   **:jules-api-client**: A pure Kotlin module responsible for all communication with the Jules API. It uses Retrofit for networking and `kotlinx.serialization` for JSON parsing.

### Core Components

-   **Jules API Integration**: The application's core functionality is powered by the Jules API. The `:jules-api-client` module handles all the details of making requests and parsing responses.
-   **Native Code Editor**: The app uses the [Sora Editor](https://github.com/Rosemoe/sora-editor), a native Android code editor, to provide a performant and feature-rich editing experience.
-   **File Management**: The app includes a file explorer for browsing project files. It uses Android's Storage Access Framework (SAF) for file system access.

## Development Conventions

### `CHANGELOG.md`

The `CHANGELOG.md` file is the source of truth for the project's history and the TODO list for future development. All changes should be reflected in this file. When you are asked to complete a task, you should look for it in the `CHANGELOG.md` TODO list and mark it as complete when you are done.

### Running Tests

The project has unit tests in the `app_android` module. To run them, use the following command from the root of the project:

```bash
./gradlew :app_android:test
```

### Version Catalogs

The project uses a Gradle Version Catalog (`gradle/libs.versions.toml`) to manage dependencies. All dependencies should be added to this file.