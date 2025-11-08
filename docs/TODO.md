This document outlines the step-by-step plan to get the Jules IDE application to a production-ready state.

---

## Phase 1: Documentation and Codebase Cleanup

*Objective: Align all documentation and code with the "Jules IDE" pivot, removing all obsolete artifacts from previous project iterations.*

- [x] Audit all documentation (`.md` files) to reflect the "Jules IDE" project.
- [x] Remove all references to "Geministrator," "ADK," and "A2A" from `README.md` and `/docs`.
- [x] Delete `Transforming an Android Device into a Standalone Pwnagotchi.md` (irrelevant Pwnagotchi project).
- [x] Delete `docs/adk_integration.md` (obsolete ADK documentation).
- [x] Consolidate and update `docs/screens.md`, `docs/workflow.md`, and `docs/task_flow.md` to use consistent terminology (e.g., "Editor Screen").
- [x] Update `README.md` and `docs/INDEX.md` to reflect the current 2-module architecture (`:app`, `:jules-api-client`).
- [ ] Investigate and remove any remaining obsolete code from the `:app` module related to the old "Geministrator" framework, ADK, or Chaquopy.

---

## Phase 2: Core IDE Feature Implementation

*Objective: Complete the essential features for the mobile IDE functionality as defined in the 2.0.0 changelog.*

- [ ] **Complete Jules API Integration**
    - [ ] Implement the UI and logic in `SourceSelectionScreen` to allow a user to select a source and be prompted to create a new session.
    - [ ] Implement the `SessionScreen` UI to display the full activity stream (conversation with the Jules API, including user prompts and agent responses).

- [ ] **Refine UI/UX**
    - [ ] Configure the Sora Editor with language-specific syntax highlighting (using the `textmate` files in `app/src/main/assets`).
    - [ ] Connect the Sora Editor to the Jules API to reflect file changes made by the agent.
    - [ ] Create a dedicated UI component (e.g., a bottom sheet or separate tab) to display terminal output from code execution via the Jules API.

- [ ] **Implement Core Actions**
    - [ ] Implement a "Run" button in the `EditorScreen` UI that triggers the execution of the currently open file via the Jules API.
    - [ ] Implement a "Commit" button to commit the current changes to the repository via the Jules API.

---

## Phase 3: Git & Version Control

*Objective: Expose on-device and remote Git operations to the user.*

- [ ] Create a UI component to display the output of `git status` from the Jules API (or the bundled JGit).
- [ ] Implement a `git diff` viewer to show changes in a selected file.
- [ ] Expose JGit's `init`, `add`, and `commit` functions through a dedicated Git management screen.

---

## Phase 4: Production Readiness

*Objective: Ensure the application is stable, tested, secure, and ready for deployment.*

- [ ] **Testing and Quality Assurance**
    - [ ] Add KDoc comments to all new and existing public classes, methods, and composables.
    - [ ] Write unit tests for all ViewModels (e.g., `SessionViewModel`, `SettingsViewModel`, `IdeViewModel`).
    - [ ] Write integration tests for the `:jules-api-client` module to verify API contracts.

- [ ] **Security and Deployment**
    - [ ] Verify that the `secrets-gradle-plugin` is fully implemented and that no API keys are stored in `local.properties` or version control.
    - [ ] Establish a CI/CD pipeline using GitHub Actions to automate builds, tests, and releases.

- [ ] **Performance**
    - [ ] Profile the app to identify and address performance bottlenecks, especially around editor loading and rendering large files.
