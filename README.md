# Geministrator

**An A2A mobile IDE powered by the Google ADK, Jules, and Gemini.**

---

## 🤖 Project Vision

Geministrator is an experimental, agent-driven mobile IDE. Its architecture is built on the **Google Agent Development Kit (ADK)** to enable complex, automated development workflows and Agent-to-Agent (A2A) communication.

## 🏗️ Current Status: Jules IDE Implementation

As a foundational step toward the full vision, the application is currently focused on providing a stable mobile IDE powered by the **Jules API**. This phase involves building the core editor, file management, and session-based interaction with the Jules API.

Future phases will re-integrate the ADK to introduce agentic features (such as automated code review) on top of this stable IDE foundation.

### Key Capabilities (Current)

-   **Jules API Integration**: Core functionality is currently powered by the Jules API for repository management and agentic tasks.
-   **Native Code Editor**: Uses the [Sora Editor](https://github.com/Rosemoe/sora-editor) for a performant code editing experience.
-   **File Management**: Includes a file explorer for browsing project files.

### Architecture

-   **:app**: The main Android application (Jetpack Compose).
-   **:jules-api-client**: Kotlin module for Jules API communication.
-   **:github-api-client**: Kotlin module for GitHub API communication.

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
