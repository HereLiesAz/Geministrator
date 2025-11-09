This document outlines the step-by-step plan to refactor the Geministrator application into a stable, production-ready state.

---

## Phase 1: Build Stabilization and Code Cleanup

*Objective: Address critical build errors, remove obsolete code, and fix architectural inconsistencies to create a stable foundation.*

- [x] **Stabilize the Build Environment**
    - [x] Remove conflicting dependencies from all `build.gradle.kts` files.
    - [x] Remove the obsolete Chaquopy Python integration.
    - [x] Synchronize the project structure.
    - [x] Execute `./gradlew clean build` and resolve all remaining dependency conflicts.

- [x] **Code Cleanup and Bug Fixes**
    - [x] Correct settings-saving bugs.
    - [x] Resolve duplicate screen definitions.
    - [x] Refactor the navigation graph.

---

## Phase 2: Core Jules IDE and A2A/ADK Integration

*Objective: Complete the essential features for the mobile IDE functionality, with the A2A/ADK integration as the core component, powered by the Jules API. The architecture should be flexible enough to support other AI providers in the future.*

- [ ] **Implement Core IDE Flow**
    - [ ] Complete the session creation logic in `SourceSelectionScreen.kt` and `JulesViewModel.kt` (this is partially done).
    - [ ] Implement the UI in `SessionScreen.kt` to display the full Jules activity stream (this is partially done).
    - [ ] Connect the Sora Editor in `IdeScreen.kt` to the `IdeViewModel` to load and save file content.

- [ ] **Implement `A2ACommunicator`**
    - [ ] Fully initialize the `A2AClient` in `A2ACommunicator` to use the Jules API.
    - [ ] Implement the real `sendMessage` logic to correctly serialize and send a prompt to the Jules API.

- [ ] **Implement UI Buttons**
    - [ ] Connect the "Run" button in `IdeScreen.kt` to the `IdeViewModel.onRunClicked()` function.
    - [ ] Connect the "Commit" button in `IdeScreen.kt` to the `IdeViewModel.onCommitClicked()` function.

- [ ] **Refine UI/UX**
    - [ ] Create a dedicated UI component (e.g., bottom sheet) in `IdeScreen.kt` to display the `consoleOutput` from the `IdeViewModel`.
    - [ ] Configure the Sora Editor with language-specific syntax highlighting.

---

## Phase 3: Production Readiness

*Objective: Ensure the application is tested, secure, and ready for deployment.*

- [ ] **Testing and Quality Assurance**
    - [ ] Write unit tests for all ViewModels (`JulesViewModel`, `SessionViewModel`, `IdeViewModel`).
    - [ ] Write integration tests for the `JulesApiClient` and `A2ACommunicator`.

- [ ] **Security and Deployment**
    - [ ] Replace `local.properties` with the `secrets-gradle-plugin` to secure API keys.
    - [ ] Establish a CI/CD pipeline using GitHub Actions to automate builds, tests, and releases.
