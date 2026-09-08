# Screens

This document provides a detailed overview of the various screens within the Jules IDE application.

## Main Screen

The `MainScreen` is the primary entry point of the application after the initial setup. It is composed of two main components:

-   **`GeministratorNavRail`**: A vertical navigation rail on the left side of the screen that allows the user to switch between the main sections of the app (e.g., "Source Selection", "Settings").
-   **`GeministratorNavHost`**: The main content area that displays the currently selected screen.

## Source Selection Screen

The `SourceSelectionScreen` is the first screen the user sees after authenticating. Its primary functions are:

-   **Displaying Sources**: It fetches and displays a list of the user's available source repositories from the Jules API.
-   **Session Creation**: When a user selects a source, a dialog prompts them to enter a title and an initial prompt to start a new session.

## Session Screen

The `SessionScreen` is where the user interacts with the Jules agent. Its key features include:

-   **Activity Stream**: It displays a chronological list of all activities within a session, including user prompts and agent responses.
-   **Message Input**: A text input field and a "Send" button at the bottom of the screen allow the user to send new messages to the agent.

## Settings Screen

The `SettingsScreen` allows the user to configure the application. Currently, it provides the following options:

-   **API Key Management**: The user can enter and save their Jules API key.
-   **Theme Selection**: The user can choose between Light, Dark, and System Default themes.

## File Explorer Screen

The `FileExplorerScreen` provides a way for the user to browse the files within a selected project. It displays a hierarchical view of the project's directory structure and allows the user to navigate through folders and open files.

## File Viewer Screen

The `FileViewerScreen` is responsible for displaying the content of a selected file. It uses the [Sora Editor](https://github.com/Rosemoe/sora-editor), a native Android code editor, to provide a rich and performant code viewing experience.