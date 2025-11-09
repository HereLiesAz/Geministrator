# Screens

This document provides a detailed overview of the various screens within the Jules IDE application.

## Main Screen

The `MainScreen` is the primary entry point of the application after the initial setup. It is composed of two main components:

-   **`GeministratorNavRail`**: A vertical navigation rail on the left side of the screen that allows the user to switch between the main sections of the app.
-   **`GeministratorNavHost`**: The main content area that displays the currently selected screen.

## Source Selection Screen

The `SourceSelectionScreen` (formerly File Explorer) is the first screen the user sees after authenticating. Its primary functions are:

-   **Displaying Sources**: It fetches and displays a list of the user's available source repositories from the Jules API.
-   **Session Creation**: When a user selects a source, a dialog prompts them to enter a title and an initial prompt to start a new session.

## Session Screen

The `SessionScreen` is where the user interacts with the Jules agent and remote A2A agents. Its key features include:

-   **Activity Stream**: It displays a chronological list of all activities within a session from the Jules API.
-   **Message Input**: A text input field and a "Send" button at the bottom of the screen allow the user to send new messages to the Jules agent, or to a remote ADK agent (e.g., via the `/gemini` command).

## Settings Screen

The `SettingsScreen` allows the user to configure the application. Currently, it provides the following options:

-   **API Key Management**: The user can enter and save their Jules API key and Gemini API key.
-   **Theme Selection**: The user can choose between Light, Dark, and System Default themes.

## IDE Screen

The `IdeScreen` is the code editor. It uses the [Sora Editor](https://github.com/Rosemoe/sora-editor), a native Android code editor, to provide a rich and performant code editing experience. It includes features like syntax highlighting, as well as "Run" and "Commit" buttons that are wired to the `IdeViewModel`.