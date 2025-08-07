# Geministrator Changelog

All notable changes to this project will be documented in this file.
## [1.1.0] - 2025-08-04

### Added

- **Persistent & Resumable Sessions**: Workflows are now saved automatically to
  `.orchestrator/session.json`. If a session is interrupted, it can be seamlessly resumed from the
  last completed step.
- **Parallel Task Execution**: The `Orchestrator` now executes sub-tasks in parallel, up to a
  configurable limit, significantly speeding up complex workflows.
- **Specification File Support**: The `run` command now accepts a `--spec-file` argument, allowing
  users to provide a formal markdown specification to guide the AI, resulting in more accurate plans
  for complex tasks.
- **Configurable Agent Behavior**: The core instructions for all AI agents are now externalized to
  `~/.gemini-orchestrator/prompts.json`. Users can edit this file to customize agent behavior
  without recompiling or reset them to default values via a `config` command.
- **Agent Prompt Editor**: A new screen has been added to the Android app, allowing users to view,
  edit, and save the `prompts.json` file that defines AI agent behavior. A reset option is also
  available to restore the default prompts.
- **Theme Switching on Android**: Users can now select between Light, Dark, and System Default
  themes in the Android app's settings screen. The selection is saved and applied across app
  restarts.
- **Dual Authentication & Free-Tier Mode**: Added support for both Google Cloud's Application
  Default Credentials (ADC) and standard API keys. A `--free-tier` configuration flag was added to
  enforce the use of ADC and free models to prevent costs.
- **Repository Cloning**: The Android app's initial setup screen now allows cloning a remote
  repository via URL, in addition to selecting a local folder.
- **Android Settings Screen**: Implemented a functional settings screen in the Android
  [cite_start]app. [cite: 74, 75]
- **API Key Management**: Users can now input and save their Gemini API key directly
  [cite_start]within the Android app's settings screen. [cite: 76]
- **Android UI Scaffolding**: Created the initial Android application shell using
  Jetpack Compose and Material 3, including themes, navigation graph, and placeholder
  [cite_start]screens. [cite: 45]
- **On-Device Backend Architecture**:
  - Implemented file system access via Android's Storage Access Framework (SAF),
    [cite_start]allowing users to grant persistent, sandboxed access to specific project
    folders. [cite: 46]
  - Integrated the `JGit` pure Kotlin library for on-device version control, enabling
    [cite_start]programmatic `init`, `add`, `commit`, and `status` operations. [cite: 47]
- **Project-Centric Workflow**: The app now operates on a user-selected project
  [cite_start]directory. [cite: 48]
- **Live Data Integration**: Replaced simulated session data with a live, interactive
  [cite_start]workflow. [cite: 50]
- **Rich Content Display**: Session logs have been enhanced to render markdown,
  formatted code blocks with custom backgrounds, and shimmering placeholder components to indicate
  [cite_start]loading states. [cite: 52]
- **UI Components**:
  - Added a functional `NavRail` using `NavigationSuiteScaffold` for primary app
    [cite_start]navigation between Sessions, Settings, and History. [cite: 53]
  - Implemented a "New Session" dialog to capture a user's high-level prompt before
    [cite_start]initiating a workflow. [cite: 54]

### Fixed

- **Gradle Build Stability**: Resolved a cascade of Gradle plugin and dependency resolution errors
  by downgrading to a stable, compatible set of versions for the Android Gradle Plugin, Kotlin, and
  Jetpack Compose. Corrected all plugin applications to use the version catalog for consistency.
- **Kotlin Compilation Errors**: Fixed multiple compilation errors in the `:app_android` module,
  including removing redeclared classes, adding the missing `material-icons-extended` dependency,
  and resolving missing import statements.
- **Critical SAF/JGit Integration**: Resolved the fundamental issue where `JGit` could not access
  file paths from Android's Storage Access Framework. Implemented a robust solution that
  [cite_start]copies the project to a local cache directory for `JGit` to operate on. [cite: 68]
- **Android `ExecutionAdapter` Implementation**: Corrected the `AndroidExecutionAdapter` to perform
  real Git operations via the `GitManager` instead of returning simulated success messages, making
  the on-device agent functional.
- **Android `Session` Data Model**: Removed the unused and uninitialized `projectUri` from the
  `Session` data class, resolving a compile-time error and clarifying that `ProjectViewModel` is the
  single source of truth for the project's location.
- **Android Studio Plugin Configuration**: Unified the plugin's configuration to exclusively use
  `PluginConfigStorage`, eliminating the conflict with `CliConfigStorage` and ensuring UI-based
  settings are correctly applied.
- **VSCode Plugin Build**: Added the `vsce` packager as a `devDependency` to `package.json`,
  allowing the Gradle build task to correctly package the `.vsix` extension file.
- **CLI API Key Validation**: Corrected the logic in the `createGeminiService` fallback method to
  ensure a newly entered API key is correctly validated and used, preventing an infinite loop on a
  previously invalid key.
- **`PauseAndExit` Command Flow**: Corrected the application control flow for the `PauseAndExit`
  command. The command is no longer prematurely handled by the `Orchestrator` and is now correctly
  passed to the `CliAdapter`, which manages the application's lifecycle.
- **Inconsistent Project Versioning**: Aligned the version numbers in `cli/build.gradle.kts` and
  `plugin_vscode/package.json` with the changelog version (`1.1.0`).

### Changed

- **Intelligent Task Triage**: The `Orchestrator` now performs an initial triage assessment on each
  task to determine if it requires web research or project context analysis. This conserves tokens
  and resources by selectively deploying the `Researcher` and `Architect` agents only when
  [cite_start]necessary. [cite: 74, 75]
- **Full `Orchestrator` Integration on Android**: Replaced the hardcoded, simulated workflow in
  `SessionViewModel` with a complete, operational call to the `Orchestrator` from the `:cli`
  module. This includes Android-specific implementations of `ILogger`,
  [cite_start]`ExecutionAdapter`, and `ConfigStorage`. [cite: 64, 65, 66]
- **Decoupled Android Project Selection**: The UI logic in `MainActivity` is now decoupled from
  project setup. The `ProjectViewModel` manages the entire project setup state, including showing
  the `ProjectSetupScreen` and handling the selected URI or cloned repository, eliminating the
  previously unresponsive UI flow.
- **Gradle Build Modernization**: Refactored the Android app's `build.gradle.kts` to use a Gradle
  [cite_start]Version Catalog (`libs.versions.toml`), centralizing dependencies and improving
  maintainability. [cite: 80]
- **Shared Library Architecture**: The `SessionViewModel` now uses the
  `ProjectViewModel` to perform real file and Git operations based on a simulated agent task
  [cite_start]list. [cite: 51]

## [1.0.0] - 2025-08-03

### Added

- **Core Agent Logic**: Initial implementation of the `Orchestrator` and the council of
  AI agents (`Architect`, `Designer`, `Antagonist`, `Researcher`, `Manager`,
  [cite_start]`TechSupport`). [cite: 55]
- **Command-Line Interface (CLI)**:
  - Created the primary CLI application in the `:cli` module for running development
    [cite_start]workflows from the terminal. [cite: 56]
  - Implemented configuration management in the user's home directory (
    [cite_start]`~/.gemini-orchestrator`). [cite: 57]
  - Added support for both Google Cloud ADC and manual API Key authentication
    [cite_start]methods. [cite: 58]
- **Multi-Module Gradle Project**:
  - Set up the initial project structure with `settings.gradle.kts` and root
    [cite_start]`build.gradle.kts`. [cite: 59]
  - Added modules for an Android App (`:app_android`), Android Studio Plugin (
    [cite_start]`:plugin-android-studio`), and VSCode Extension (`:plugin-vscode`). [cite: 60]
- **Plugin Scaffolding**:
  - Created basic build scripts and UI shells for the Android Studio and VSCode
    [cite_start]plugins. [cite: 61]
  - Established the architecture where plugins depend on the `:cli` module as a shared
    [cite_start]library. [cite: 62]
---

## TODO

### Core Logic & On-Device Backend

- [ ] **Hardcoded Project Context in Android/Plugin**
- [ ] **Robust Error Handling**
  - [ ] In `ProjectManager`, add detailed `try-catch` blocks for all SAF file operations to handle
    `FileNotFoundException`, `IOException`, etc.
  - [ ] In `GitManager`, wrap all `JGit` calls in `try-catch` blocks to handle exceptions like
    `NoHeadException`, `GitAPIException`, etc.
  - [ ] Propagate errors gracefully to the UI, displaying user-friendly messages in the
    [cite_start]session log (e.g., using a new `LogEntry` type for errors). [cite: 67]
- [ ] **GitHub Integration**
  - [ ] [cite_start]Add a new `GitHubManager` class to the `data` package. [cite: 69]
  - [ ] [cite_start]Integrate a GitHub API client library (e.g., `kotlin-github-api`). [cite: 70]
    - [ ] Implement OAuth for user authentication with GitHub.
  - [ ] [cite_start]Add methods for `createPullRequest`, `listIssues`, and `createIssue`. [cite: 71]
  - [ ] Connect these methods to new `AbstractCommand`s that the `Orchestrator` can
    [cite_start]generate. [cite: 72]

### Android App UI/UX
- [ ] **Settings Screen**
  - [x] [cite_start]Implement UI for changing the color theme. [cite: 76]
  - [x] Add a section to view and edit the JSON prompts used by the AI
    [cite_start]agents. [cite: 77]
- [ ] **History Screen**
  - [ ] [cite_start]Design a UI to display a list of completed sessions. [cite: 78]
  - [ ] [cite_start]Implement persistence for session logs (e.g., using Room database). [cite: 79]
  - [ ] [cite_start]Allow users to tap on a past session to view its full log. [cite: 80]
- [ ] **Git UI Integration**
  - [ ] [cite_start]Create a "Version Control" tab or panel within the `SessionScreen`. [cite: 81]
  - [ ] [cite_start]Display the output of `git status` in a formatted way. [cite: 82]
  - [ ] Add a "Staged Files" section with checkboxes, allowing the user to manually
    [cite_start]stage or unstage files. [cite: 83]
  - [ ] [cite_start]Implement a `git diff` viewer to show changes in a selected file. [cite: 84]
- [ ] **File Browser**
  - [ ] [cite_start]Create a new "Explorer" screen accessible from the `NavRail`. [cite: 85]
  - [ ] Display a navigable tree of the files and directories within the user-selected
    [cite_start]project folder. [cite: 86]
  - [ ] Allow users to tap on a file to view its content in a read-only
    [cite_start]editor. [cite: 87]
- [ ] **Real-time Sync & UI State**
  - [ ] Convert `SessionViewModel` to emit a `UiState` object containing the log
    [cite_start]entries, current agent status, etc., to better manage loading and result
    states. [cite: 88]
  - [ ] Refactor the `Orchestrator`'s `ILogger` to emit structured `LogEntry` objects
    [cite_start]instead of raw strings to the `Flow`. [cite: 89]

### Plugins
- [ ] **VSCode Backend Service**
  - [ ] Define the specific REST/WebSocket API endpoints needed (e.g., `/run`,
    [cite_start]`/status`, `/ws/logs`). [cite: 90]
  - [ ] [cite_start]Choose a framework (Ktor, Spring Boot) and implement the server. [cite: 91]
  - [ ] The server must be able to launch the `Orchestrator` from the `:cli` module as a
    [cite_start]child process or library call. [cite: 92]
  - [ ] Update the `extension.ts` in `:plugin-vscode` to communicate with this local
    [cite_start]server instead of spawning the CLI directly. [cite: 93]
- [ ] **Plugin Feature Parity**
  - [ ] Implement the `NewSessionDialog` equivalent in the Android Studio plugin
    [cite_start]UI. [cite: 94]
  - [ ] Ensure the Android Studio plugin can display markdown, code blocks, and
    [cite_start]placeholders. [cite: 95]
  - [ ] Replicate the `NavRail` functionality (Sessions, Settings, History) in the
    [cite_start]Android Studio plugin's tool window. [cite: 96]

### Code Quality & Refinements
- [ ] **Unit & Integration Tests**
  - [ ] [cite_start]Write unit tests for `GitManager` using a temporary folder. [cite: 97]
  - [ ] [cite_start]Write unit tests for all `ViewModel` state logic. [cite: 98]
  - [ ] Add integration tests for the `Orchestrator` running with a mock
    [cite_start]`ExecutionAdapter`. [cite: 99]
- [ ] **Documentation**
  - [ ] [cite_start]Add KDoc comments to all public classes and methods. [cite: 100]
  - [ ] Update the main `README.md` to include instructions for setting up and running
    [cite_start]the Android app. [cite: 101]
- [ ] **Performance Optimization**
  - [ ] Profile the app during file I/O and Git operations to identify
    [cite_start]bottlenecks. [cite: 102]
  - [ ] [cite_start]Optimize the `LazyColumn` in `SessionScreen` for very long logs. [cite: 103]