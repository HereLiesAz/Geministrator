# Geministrator Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - (In Progress)

### Added

- **Android UI Scaffolding**: Created the initial Android application shell using Jetpack Compose
  and Material 3, including themes, navigation graph, and placeholder screens.
- **On-Device Backend Architecture**:
    - Implemented file system access via Android's Storage Access Framework (SAF), allowing users to
      grant persistent, sandboxed access to specific project folders.
    - Integrated the `JGit` pure Kotlin library for on-device version control, enabling programmatic
      `init`, `add`, `commit`, and `status` operations.
- **Project-Centric Workflow**: The app now operates on a user-selected project directory. A setup
  screen guides the user through this one-time selection process.
- **Live Data Integration**: Replaced simulated session data with a live, interactive workflow. The
  `SessionViewModel` now uses the `ProjectViewModel` to perform real file and Git operations based
  on a simulated agent task list.
- **Rich Content Display**: Session logs have been enhanced to render markdown, formatted code
  blocks with custom backgrounds, and shimmering placeholder components to indicate loading states.
- **UI Components**:
    - Added a functional `NavRail` using `NavigationSuiteScaffold` for primary app navigation
      between Sessions, Settings, and History.
    - Implemented a "New Session" dialog to capture a user's high-level prompt before initiating a
      workflow.

## [1.0.0] - 2025-08-03

### Added

- **Core Agent Logic**: Initial implementation of the `Orchestrator` and the council of AI agents (
  `Architect`, `Designer`, `Antagonist`, `Researcher`, `Manager`, `TechSupport`).
- **Command-Line Interface (CLI)**:
    - Created the primary CLI application in the `:cli` module for running development workflows
      from the terminal.
    - Implemented configuration management in the user's home directory (`~/.gemini-orchestrator`).
    - Added support for both Google Cloud ADC and manual API Key authentication methods.
- **Multi-Module Gradle Project**:
    - Set up the initial project structure with `settings.gradle.kts` and root `build.gradle.kts`.
    - Added modules for an Android App (`:app_android`), Android Studio Plugin (
      `:plugin-android-studio`), and VSCode Extension (`:plugin-vscode`).
- **Plugin Scaffolding**:
    - Created basic build scripts and UI shells for the Android Studio and VSCode plugins.
    - Established the architecture where plugins depend on the `:cli` module as a shared library.

---

## TODO

### Core Logic & On-Device Backend

- [ ] **Full `Orchestrator` Integration**
    - [ ] Replace the hardcoded workflow in `SessionViewModel` with a call to the real
      `Orchestrator` instance from the `:cli` module.
    - [ ] Create a `ViewModel`-safe `ExecutionAdapter` for Android that uses the `ProjectManager`
      for file operations.
    - [ ] Implement a `Flow`-based `ILogger` that the `Orchestrator` can use to stream log entries
      back to the `SessionViewModel` in real-time.
    - [ ] Pass the on-device `GitManager` to the `Orchestrator`'s `ExecutionAdapter` to handle
      version control commands.
- [ ] **Robust Error Handling**
    - [ ] In `ProjectManager`, add detailed `try-catch` blocks for all SAF file operations to handle
      `FileNotFoundException`, `IOException`, etc.
    - [ ] In `GitManager`, wrap all `JGit` calls in `try-catch` blocks to handle exceptions like
      `NoHeadException`, `GitAPIException`, etc.
    - [ ] Propagate errors gracefully to the UI, displaying user-friendly messages in the session
      log (e.g., using a new `LogEntry` type for errors).
- [ ] **SAF-to-File Path Resolution**
    - [ ] Implement a reliable mechanism to translate a SAF `Uri` to a real `File` path that `JGit`
      can use directly, likely by copying the repository to the app's internal cache directory and
      syncing changes.
- [ ] **GitHub Integration**
    - [ ] Add a new `GitHubManager` class to the `data` package.
    - [ ] Integrate a GitHub API client library (e.g., `kotlin-github-api`).
    - [ ] Implement OAuth for user authentication with GitHub.
    - [ ] Add methods for `createPullRequest`, `listIssues`, and `createIssue`.
    - [ ] Connect these methods to new `AbstractCommand`s that the `Orchestrator` can generate.

### Android App UI/UX

- [ ] **Settings Screen**
    - [ ] Design the UI layout for managing settings.
    - [ ] Create a `SettingsViewModel` to load and save preferences.
    - [ ] Add a field for the user to input and save their Gemini API key.
    - [ ] Implement UI for changing the color theme.
    - [ ] Add a section to view and edit the JSON prompts used by the AI agents.
- [ ] **History Screen**
    - [ ] Design a UI to display a list of completed sessions.
    - [ ] Implement persistence for session logs (e.g., using Room database).
    - [ ] Allow users to tap on a past session to view its full log.
- [ ] **Git UI Integration**
    - [ ] Create a "Version Control" tab or panel within the `SessionScreen`.
    - [ ] Display the output of `git status` in a formatted way.
    - [ ] Add a "Staged Files" section with checkboxes, allowing the user to manually stage or
      unstage files.
    - [ ] Implement a `git diff` viewer to show changes in a selected file.
- [ ] **File Browser**
    - [ ] Create a new "Explorer" screen accessible from the `NavRail`.
    - [ ] Display a navigable tree of the files and directories within the user-selected project
      folder.
    - [ ] Allow users to tap on a file to view its content in a read-only editor.
- [ ] **Real-time Sync & UI State**
    - [ ] Convert `SessionViewModel` to emit a `UiState` object containing the log entries, current
      agent status, etc., to better manage loading and result states.
    - [ ] Refactor the `Orchestrator`'s `ILogger` to emit structured `LogEntry` objects instead of
      raw strings to the `Flow`.

### Plugins

- [ ] **VSCode Backend Service**
    - [ ] Define the specific REST/WebSocket API endpoints needed (e.g., `/run`, `/status`,
      `/ws/logs`).
    - [ ] Choose a framework (Ktor, Spring Boot) and implement the server.
    - [ ] The server must be able to launch the `Orchestrator` from the `:cli` module as a child
      process or library call.
    - [ ] Update the `extension.ts` in `:plugin-vscode` to communicate with this local server
      instead of spawning the CLI directly.
- [ ] **Plugin Feature Parity**
    - [ ] Implement the `NewSessionDialog` equivalent in the Android Studio plugin UI.
    - [ ] Ensure the Android Studio plugin can display markdown, code blocks, and placeholders.
    - [ ] Replicate the `NavRail` functionality (Sessions, Settings, History) in the Android Studio
      plugin's tool window.

### Code Quality & Refinements

- [ ] **Unit & Integration Tests**
    - [ ] Write unit tests for `GitManager` using a temporary folder.
    - [ ] Write unit tests for all `ViewModel` state logic.
    - [ ] Add integration tests for the `Orchestrator` running with a mock `ExecutionAdapter`.
- [ ] **Documentation**
    - [ ] Add KDoc comments to all public classes and methods.
    - [ ] Update the main `README.md` to include instructions for setting up and running the Android
      app.
- [ ] **Performance Optimization**
    - [ ] Profile the app during file I/O and Git operations to identify bottlenecks.
    - [ ] Optimize the `LazyColumn` in `SessionScreen` for very long logs.