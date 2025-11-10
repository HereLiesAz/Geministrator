# Geministrator

**An A2A (Agent-to-Agent) mobile client for the Jules API and remote Google ADK agents.**

---

## 🤖 Project Vision

Geministrator is an experimental, agent-driven mobile IDE. Its architecture is designed to act as a central hub for interacting with multiple, distinct AI agents:

1.  **The Jules API:** Used for all core, in-session development tasks (code generation, file operations, execution, commits).
2.  **Remote ADK Agents:** Interacted with via the **A2A (Agent-to-Agent) protocol**. This allows the app to offload complex, specialized tasks (like project-wide analysis, code reviews, or planning) to dedicated agents.

## 🏗️ Current Status: Jules IDE Implementation

As a foundational step, the application is currently focused on providing a stable mobile IDE powered by the **Jules API**. Future phases will fully integrate the A2A client to enable communication with the remote ADK, as described in `docs/TODO.md`.

### Key Capabilities

-   **Jules API Integration**: Core functionality is powered by the Jules API for repository management and agentic coding tasks.
-   **A2A Protocol Client**: Includes the `a2a-java-sdk-client` for future communication with remote ADK agents.
-   **Native Code Editor**: Uses the [Sora Editor](https://github.com/Rosemoe/sora-editor).
-   **File Management**: Provides a file explorer for browsing project files.

### Architecture

-   **:app**: The main Android application (Jetpack Compose).
-   **:jules-api-client**: A pure Kotlin module for all Jules API communication.
-   **:github-api-client**: A pure Kotlin module for GitHub API communication (e.g., for ADK-powered code review agents).

---

## 🚀 Getting Started

### Prerequisites

-   **Android 8.0 (API 26) or higher**
-   **A Jules API Key**
-   (Optional) An endpoint for an A2A-compatible remote agent.

### Installation

1.  **Build the APK:**
    ```bash
    ./gradlew :app:assembleDebug
    ```
2.  **Install the APK:**
    The APK is in `app/build/outputs/apk/debug/`.

---

## ⚙️ Configuration

1.  Open the app and navigate to **Settings**.
2.  Enter your Jules API key.
3.  (Optional) Enter your Gemini API key for on-device generation tasks.
4.  Tap "Save Settings".

---

## 💻 Usage (Current)

1.  The app will display your available source repositories from the Jules API.
2.  Select a repository to start a new session.
3.  You can then browse files and interact with the Jules agent.
4.  Typing `/gemini` in the chat will route your message to the A2A-connected agent (if configured).

---

## 📜 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.