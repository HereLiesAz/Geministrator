# Geministrator

**An A2A mobile IDE powered by the Jules and Gemini.**

---

## 🤖 Key Capabilities

- **Jules API Integration**: The application's core functionality is powered by the Jules API, allowing you to automate and enhance your software development lifecycle.
- **Native Code Editor**: The app uses the [Sora Editor](https://github.com/Rosemoe/sora-editor), a native Android code editor, to provide a performant and feature-rich editing experience.
- **File Management**: The app includes a file explorer for browsing project files. It uses Android's Storage Access Framework (SAF) for file system access.
- **Git Integration**: The app uses JGit to provide basic Git functionality, such as viewing the status of your repository.

---

## 🏗️ Architecture

The project is a multi-module Gradle project with the following structure:

-   **:app_android**: The main Android application, built with Jetpack Compose. It contains all the UI and application logic.
-   **:jules-api-client**: A pure Kotlin module responsible for all communication with the Jules API. It uses Retrofit for networking and `kotlinx.serialization` for JSON parsing.

---

## 🚀 Getting Started

### Prerequisites

* **Android 8.0 (API 26) or higher**
* **A Jules API Key**

### Installation

1.  **Build the APK:**
    From the root of the project, run the following command:
    ```bash
    ./gradlew :app_android:assembleDebug
    ```
2.  **Install the APK:**
    Install the generated APK on your Android device. The APK will be located in `app_android/build/outputs/apk/debug/`.

---

## ⚙️ Configuration

The first time you run the application, you will need to enter your Jules API key in the settings screen.

1.  Open the app and navigate to the **Settings** screen.
2.  Enter your Jules API key in the "Jules API Key" field.
3.  Tap "Save Settings".

---

## 💻 Usage

Once you have configured your API key, you can start using the application.

1.  The application will display a list of your available source repositories from the Jules API.
2.  Select a repository to work with.
3.  You can then browse the files in the repository and open them in the code editor.

---

## 📜 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
