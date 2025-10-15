This document outlines the step-by-step plan to refactor the Geministrator application into a stable, production-ready state, and then to re-introduce agentic features using the Google Agent Development Kit (ADK).

---

## Phase 1: Build Stabilization and Code Cleanup

*Objective: Address critical build errors, remove obsolete code, and fix architectural inconsistencies to create a stable foundation.*

- [x] **Stabilize the Build Environment**
    - [x] Remove conflicting `google-adk` dependencies from all `build.gradle.kts` files.
    - [x] Remove the obsolete Chaquopy Python integration.
        - [x] Delete the `app/src/main/python` directory.
        - [x] Remove the `chaquopy` plugin and configuration block from `app/build.gradle.kts`.
        - [x] Delete `CliScreen.kt` and `CliViewModel.kt`.
    - [x] Synchronize the project structure by removing the `:prompts` module from `settings.gradle.kts`.
    - [x] Execute `./gradlew clean build` and resolve any remaining dependency conflicts.

- [x] **Code Cleanup and Bug Fixes**
    - [x] Correct the settings-saving bug in `SettingsViewModel.kt` where the GCP Project ID is saved to the wrong DataStore key.
    - [x] Resolve duplicate screen definitions by renaming `ui/ide/IDEScreen.kt` to `ui/ide/SearchScreen.kt` and its contained composable.
    - [x] Refactor the navigation graph in `GeministratorNavHost.kt` to include all destinations from the `NavRail` and add a route for the code editor.

---

## Phase 2: Core IDE Feature Implementation

*Objective: Complete the essential features for the mobile IDE functionality.*

- [x] **Implement Core IDE Flow**
    - [x] Complete the session creation logic in `SourceSelectionScreen.kt`.
    - [x] Implement the UI in `SessionScreen.kt` to display the activity stream.
    - [x] Connect the Sora Editor in `IdeScreen.kt` to the Jules API to reflect file changes from agent activities.
    - [ ] Implement UI buttons for core actions like "Run" and "Commit" and connect them to ViewModel functions.

---

## Phase 3: ADK Integration and Agentic Features

*Objective: Re-introduce agent capabilities using the ADK on a stable foundation, starting with a high-value feature.*

- [ ] **Integrate ADK for a Target Feature: Automated Code Review**
    - [ ] Define a new `CodeReviewAgent` using the ADK.
    - [ ] Create tool wrappers for existing `GitHubApiClient.kt` methods (`getPullRequests`, `getPullRequestDiff`, `createComment`).
    - [ ] Develop a `CodeReviewService.kt` to manage the `AdkApp` runner and invoke the agent.
    - [ ] Create a new UI screen for users to trigger the code review service.

---

## Phase 4: Production Readiness

*Objective: Ensure the application is tested, secure, and ready for deployment.*

- [ ] **Testing and Quality Assurance**
    - [ ] Write unit tests for all ViewModels and Repositories.
    - [ ] Write integration tests for the Jules and Gemini API clients.

- [ ] **Security and Deployment**
    - [ ] Replace `local.properties` with the `secrets-gradle-plugin` to secure API keys.
    - [ ] Establish a CI/CD pipeline using GitHub Actions to automate builds, tests, and releases.
