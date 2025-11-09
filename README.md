# Geministrator

**An A2A (Agent-to-Agent) mobile client for the Jules API.**

---

## 🤖 Project Vision

Geministrator is an experimental, agent-driven mobile IDE. Its architecture is designed to act as a central hub for interacting with the Jules API via the A2A (Agent-to-Agent) protocol. This allows the app to offload complex, specialized tasks (like project-wide analysis, code reviews, or planning) to the Jules API.

## 🏗️ Current Status: Jules IDE Implementation

The application is currently focused on providing a stable mobile IDE powered by the **Jules API**.

### Key Capabilities

-   **Jules API Integration**: Core functionality is powered by the Jules API for repository management and agentic coding tasks.
-   **A2A Protocol Client**: Includes the `a2a-java-sdk-client` for communication with the Jules API.
-   **Native Code Editor**: Uses the [Sora Editor](https://github.com/Rosemoe/sora-editor).
-   **File Management**: Provides a file explorer for browsing project files.

### Architecture

-   **:app**: The main Android application (Jetpack Compose).
-   **:jules-api-client**: A pure Kotlin module for all Jules API communication.
-   **:github-api-client**: A pure Kotlin module for GitHub API communication.

---

## 🚀 Getting Started

### Prerequisites

-   **Android 8.0 (API 26) or higher**
-   **A Jules API Key**

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
3.  Tap "Save Settings".

---

## 💻 Usage (Current)

1.  The app will display your available source repositories from the Jules API.
2.  Select a repository to start a new session.
3.  You can then browse files and interact with the Jules agent.

---

## 📜 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
