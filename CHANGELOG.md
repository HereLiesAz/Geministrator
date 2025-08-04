# Geministrator Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2025-08-04

### Added

- [cite_start]**Persistent & Resumable Sessions**: Workflows are now saved automatically to
  `.orchestrator/session.json`. [cite: 32] [cite_start]If a session is interrupted, it can be
  seamlessly resumed from the last completed step. [cite: 33]
- [cite_start]**Parallel Task Execution**: The `Orchestrator` now executes sub-tasks in parallel, up
  to a configurable limit, significantly speeding up complex workflows. [cite: 164]
- [cite_start]**Specification File Support**: The `run` command now accepts a `--spec-file`
  argument, allowing users to provide a formal markdown specification to guide the AI, resulting in
  more accurate plans for complex tasks. [cite: 27, 28]
- [cite_start]**Configurable Agent Behavior**: The core instructions for all AI agents are now
  externalized to `~/.gemini-orchestrator/prompts.json`. [cite: 35] [cite_start]Users can edit this
  file to customize agent behavior without recompiling [cite: 36] [cite_start]or reset them to
  default values via a `config` command. [cite: 38]
- [cite_start]**Dual Authentication & Free-Tier Mode**: Added support for both Google Cloud's
  Application Default Credentials (ADC) and standard API keys. [cite: 7] [cite_start]A `--free-tier`
  configuration flag was added to enforce the use of ADC and free models to prevent
  costs. [cite: 8, 24]
- [cite_start]**Repository Cloning**: The Android app's initial setup screen now allows cloning a
  remote repository via URL, in addition to selecting a local folder. [cite: 425]
- [cite_start]**Android Settings Screen**: Implemented a functional settings screen in the Android
  app. [cite: 379, 380, 381]
- [cite_start]**API Key Management**: Users can now input and save their Gemini API key directly
  within the Android app's settings screen. [cite: 78, 407]
- [cite_start]**Android UI Scaffolding**: Created the initial Android application shell using
  Jetpack Compose and Material 3, including themes, navigation graph, and placeholder
  screens. [cite: 48]
- **On-Device Backend Architecture**:
    - [cite_start]Implemented file system access via Android's Storage Access Framework (SAF),
      allowing users to grant persistent, sandboxed access to specific project folders. [cite: 49]
    - [cite_start]Integrated the `JGit` pure Kotlin library for on-device version control, enabling
      programmatic `init`, `add`, `commit`, and `status` operations. [cite: 50]
- [cite_start]**Project-Centric Workflow**: The app now operates on a user-selected project
  directory. [cite: 51]
- [cite_start]**Live Data Integration**: Replaced simulated session data with a live, interactive
  workflow. [cite: 53]
- [cite_start]**Rich Content Display**: Session logs have been enhanced to render markdown,
  formatted code blocks with custom backgrounds, and shimmering placeholder components to indicate
  loading states. [cite: 55]
- **UI Components**:
    - [cite_start]Added a functional `NavRail` using `NavigationSuiteScaffold` for primary app
      navigation between Sessions, Settings, and History. [cite: 56]
    - [cite_start]Implemented a "New Session" dialog to capture a user's high-level prompt before
      initiating a workflow. [cite: 57]

### Fixed

- [cite_start]**Critical SAF/JGit Integration**: Resolved the fundamental issue where `JGit` could
  not access file paths from Android's Storage Access Framework. [cite: 71] [cite_start]Implemented
  a robust solution that copies the project to a local cache directory for `JGit` to operate
  on. [cite: 71, 431]
- [cite_start]**Android `ExecutionAdapter` Implementation**: Corrected the `AndroidExecutionAdapter`
  to perform real Git operations via the `GitManager` instead of returning simulated success
  messages, making the on-device agent functional. [cite: 451, 452]
- **Android `Session` Data Model**: Removed the unused and uninitialized `projectUri` from the
  `Session` data class, resolving a compile-time error and clarifying that `ProjectViewModel` is the
  single source of truth for the project's location.
- [cite_start]**Android Studio Plugin Configuration**: Unified the plugin's configuration to
  exclusively use `PluginConfigStorage`, eliminating the conflict with `CliConfigStorage` and
  ensuring UI-based settings are correctly applied. [cite: 468, 470, 474]
- [cite_start]**VSCode Plugin Build**: Added the `vsce` packager as a `devDependency` to
  `package.json`, allowing the Gradle build task to correctly package the `.vsix` extension
  file. [cite: 497]
- [cite_start]**CLI API Key Validation**: Corrected the logic in the `createGeminiService` fallback
  method to ensure a newly entered API key is correctly validated and used, preventing an infinite
  loop on a previously invalid key. [cite: 140, 141]
- [cite_start]**`PauseAndExit` Command Flow**: Corrected the application control flow for the
  `PauseAndExit` command. [cite: 149] [cite_start]The command is no longer prematurely handled by
  the `Orchestrator` and is now correctly passed to the `CliAdapter`, which manages the
  application's lifecycle. [cite: 288]
- [cite_start]**Inconsistent Project Versioning**: Aligned the version numbers in
  `cli/build.gradle.kts` [cite: 109] [cite_start]and
  `plugin_vscode/package.json` [cite: 491] [cite_start]with the changelog version (
  `1.1.0`). [cite: 47]

### Changed

- [cite_start]**Intelligent Task Triage**: The `Orchestrator` now performs an initial triage
  assessment on each task to determine if it requires web research or project context
  analysis. [cite: 189, 323] [cite_start]This conserves tokens and resources by selectively
  deploying the `Researcher` and `Architect` agents only when necessary. [cite: 3, 190, 191]
- [cite_start]**Full `Orchestrator` Integration on Android**: Replaced the hardcoded, simulated
  workflow in `SessionViewModel` with a complete, operational call to the `Orchestrator` from the
  `:cli` module. [cite: 66] [cite_start]This includes Android-specific implementations of `ILogger`,
  `ExecutionAdapter`, and `ConfigStorage`. [cite: 370, 375]
- [cite_start]**Decoupled Android Project Selection**: The UI logic in `MainActivity` is now
  decoupled from project setup. [cite: 337] [cite_start]The `ProjectViewModel` manages the entire
  project setup state, including showing the `ProjectSetupScreen` and handling the selected URI or
  cloned repository, eliminating the previously unresponsive UI flow. [cite: 391, 393, 397]
- [cite_start]**Gradle Build Modernization**: Refactored the Android app's `build.gradle.kts` to use
  a Gradle Version Catalog (`libs.versions.toml`), centralizing dependencies and improving
  maintainability. [cite: 328, 331, 332, 333, 107]
- [cite_start]**Shared Library Architecture**: The `SessionViewModel` now uses the
  `ProjectViewModel` to perform real file and Git operations based on a simulated agent task
  list. [cite: 54]

## [1.0.0] - 2025-08-03

### Added

- [cite_start]**Core Agent Logic**: Initial implementation of the `Orchestrator` and the council of
  AI agents (`Architect`, `Designer`, `Antagonist`, `Researcher`, `Manager`,
  `TechSupport`). [cite: 58]
- **Command-Line Interface (CLI)**:
    - [cite_start]Created the primary CLI application in the `:cli` module for running development
      workflows from the terminal. [cite: 59]
    - [cite_start]Implemented configuration management in the user's home directory (
      `~/.gemini-orchestrator`). [cite: 60]
    - [cite_start]Added support for both Google Cloud ADC and manual API Key authentication
      methods. [cite: 61]
- **Multi-Module Gradle Project**:
    - [cite_start]Set up the initial project structure with `settings.gradle.kts` and root
      `build.gradle.kts`. [cite: 62]
    - [cite_start]Added modules for an Android App (`:app_android`), Android Studio Plugin (
      `:plugin-android-studio`), and VSCode Extension (`:plugin-vscode`). [cite: 63]
- **Plugin Scaffolding**:
    - [cite_start]Created basic build scripts and UI shells for the Android Studio and VSCode
      plugins. [cite: 64]
    - [cite_start]Established the architecture where plugins depend on the `:cli` module as a shared
      library. [cite: 65]
---

## TODO

### Core Logic & On-Device Backend
- [ ] **Robust Error Handling**
    - [ ] In `ProjectManager`, add detailed `try-catch` blocks for all SAF file operations to handle
      `FileNotFoundException`, `IOException`, etc.
    - [ ] In `GitManager`, wrap all `JGit` calls in `try-catch` blocks to handle exceptions like
      `NoHeadException`, `GitAPIException`, etc.
    - [ ] [cite_start]Propagate errors gracefully to the UI, displaying user-friendly messages in
      the session log (e.g., using a new `LogEntry` type for errors). [cite: 70]
- [ ] **GitHub Integration**
    - [ ] [cite_start]Add a new `GitHubManager` class to the `data` package. [cite: 72]
    - [ ] [cite_start]Integrate a GitHub API client library (e.g., `kotlin-github-api`). [cite: 73]
    - [ ] Implement OAuth for user authentication with GitHub.
    - [ ] [cite_start]Add methods for `createPullRequest`, `listIssues`, and
      `createIssue`. [cite: 74]
    - [ ] [cite_start]Connect these methods to new `AbstractCommand`s that the `Orchestrator` can
      generate. [cite: 75]

### Android App UI/UX
- [ ] **Settings Screen**
    - [ ] [cite_start]Implement UI for changing the color theme. [cite: 79]
    - [ ] [cite_start]Add a section to view and edit the JSON prompts used by the AI
      agents. [cite: 80]
- [ ] **History Screen**
    - [ ] [cite_start]Design a UI to display a list of completed sessions. [cite: 81]
    - [ ] [cite_start]Implement persistence for session logs (e.g., using Room database). [cite: 82]
    - [ ] [cite_start]Allow users to tap on a past session to view its full log. [cite: 83]
- [ ] **Git UI Integration**
    - [ ] [cite_start]Create a "Version Control" tab or panel within the `SessionScreen`. [cite: 84]
    - [ ] [cite_start]Display the output of `git status` in a formatted way. [cite: 85]
    - [ ] [cite_start]Add a "Staged Files" section with checkboxes, allowing the user to manually
      stage or unstage files. [cite: 86]
    - [ ] [cite_start]Implement a `git diff` viewer to show changes in a selected file. [cite: 87]
- [ ] **File Browser**
    - [ ] [cite_start]Create a new "Explorer" screen accessible from the `NavRail`. [cite: 88]
    - [ ] [cite_start]Display a navigable tree of the files and directories within the user-selected
      project folder. [cite: 89]
    - [ ] [cite_start]Allow users to tap on a file to view its content in a read-only
      editor. [cite: 90]
- [ ] **Real-time Sync & UI State**
    - [ ] [cite_start]Convert `SessionViewModel` to emit a `UiState` object containing the log
      entries, current agent status, etc., to better manage loading and result states. [cite: 91]
    - [ ] [cite_start]Refactor the `Orchestrator`'s `ILogger` to emit structured `LogEntry` objects
      instead of raw strings to the `Flow`. [cite: 92]

### Plugins
- [ ] **VSCode Backend Service**
    - [ ] [cite_start]Define the specific REST/WebSocket API endpoints needed (e.g., `/run`,
      `/status`, `/ws/logs`). [cite: 93]
    - [ ] [cite_start]Choose a framework (Ktor, Spring Boot) and implement the server. [cite: 94]
    - [ ] [cite_start]The server must be able to launch the `Orchestrator` from the `:cli` module as
      a child process or library call. [cite: 95]
    - [ ] [cite_start]Update the `extension.ts` in `:plugin-vscode` to communicate with this local
      server instead of spawning the CLI directly. [cite: 96]
- [ ] **Plugin Feature Parity**
    - [ ] [cite_start]Implement the `NewSessionDialog` equivalent in the Android Studio plugin
      UI. [cite: 97]
    - [ ] [cite_start]Ensure the Android Studio plugin can display markdown, code blocks, and
      placeholders. [cite: 98]
    - [ ] [cite_start]Replicate the `NavRail` functionality (Sessions, Settings, History) in the
      Android Studio plugin's tool window. [cite: 99]

### Code Quality & Refinements
- [ ] **Unit & Integration Tests**
    - [ ] [cite_start]Write unit tests for `GitManager` using a temporary folder. [cite: 100]
    - [ ] [cite_start]Write unit tests for all `ViewModel` state logic. [cite: 101]
    - [ ] [cite_start]Add integration tests for the `Orchestrator` running with a mock
      `ExecutionAdapter`. [cite: 102]
- [ ] **Documentation**
    - [ ] [cite_start]Add KDoc comments to all public classes and methods. [cite: 103]
    - [ ] [cite_start]Update the main `README.md` to include instructions for setting up and running
      the Android app. [cite: 104]
- [ ] **Performance Optimization**
    - [ ] [cite_start]Profile the app during file I/O and Git operations to identify
      bottlenecks. [cite: 105]
    - [ ] [cite_start]Optimize the `LazyColumn` in `SessionScreen` for very long logs. [cite: 106]