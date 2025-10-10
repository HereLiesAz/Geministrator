# Jules IDE Changelog

All notable changes to this project will be documented in this file.

## [2.0.0] - 2025-10-04

### Added

-   **Architectural Overhaul**: The application has been completely reworked from the ground up to be a mobile IDE powered by the Jules API. The old "Geministrator" AI agent framework has been removed entirely.
-   **Jules API Integration**:
    -   A new `:jules-api-client` module has been created to handle all communication with the Jules API.
    -   The application now fetches and displays a list of the user's available source repositories from the Jules API.
-   **Native Code Editor**: The app now uses the [Sora Editor](https://github.com/Rosemoe/sora-editor), a native Android code editor, to provide a performant and feature-rich editing experience.
-   **File Explorer**: The app includes a functional file explorer for browsing project files.

### Changed

-   The entire UI has been redesigned to support the new IDE workflow.
-   The settings screen has been simplified to manage the Jules API key and the application theme.

### Removed

-   The `:cli`, `:prompts`, `:plugin_android_studio`, and `:plugin_vscode` modules have been removed.
-   All code related to the old "Geministrator" AI agent framework has been removed.

---

## TODO

### High Priority

-   [x] **Resolve Build Issues**: The application is currently unbuildable due to a persistent Gradle dependency conflict. This is the most critical and immediate task.
-   [ ] **Complete Jules API Integration**:
    -   [ ] **Session Creation**: Implement the UI and logic to allow the user to select a source from the `SourceSelectionScreen` and then create a new session with a prompt.
    -   [ ] **Activity Stream**: Create the main UI for displaying the conversation with the Jules API, including user prompts and agent responses.

### Medium Priority

-   [ ] **Refine the UI/UX**:
    -   [ ] **Code Editor**: Configure the Sora Editor with language-specific syntax highlighting and connect it to the Jules API to reflect changes made by the agent.
    -   [ ] **Terminal/Output View**: Create a dedicated UI component to display the output of commands and code execution from the Jules API.
-   [ ] **Implement a "Run" Button**: Add a "Run" button to the UI that triggers the execution of the currently open file in the editor via the Jules API.
-   [ ] **Add a "Commit" Button**: Implement a button to commit the current changes to the repository via the Jules API.

### Low Priority

-   [ ] **Add a "Git Status" View**: Create a UI component to display the output of `git status` from the Jules API.
-   [ ] **Implement a `git diff` Viewer**: Add a feature to show the changes in a selected file.
-   [ ] **Add KDoc Comments**: Add KDoc comments to all public classes and methods.
-   [ ] **Write Unit and Integration Tests**: Once the build is fixed, the application needs a suite of unit and integration tests to ensure its stability and correctness.
-   [ ] **Performance Optimization**: Profile the app to identify and address any performance bottlenecks.