# UI/UX

This document provides an overview of the User Interface (UI) and User Experience (UX) design principles used in the Jules IDE application.

## UI Components

The application is built with Jetpack Compose and Material 3, using the following key components:

- **`AzNavRail`**: A vertical navigation rail for primary navigation between the main sections of
  the app.
-   **`Sora Editor`**: A native Android code editor that provides a rich and performant code editing experience.
-   **`LazyColumn`**: Used to display lists of items, such as the list of source repositories and the activity stream in a session.
-   **`Dialog`**: Used to create the "New Session" dialog.
-   **`Scaffold`**: Provides a standard layout structure for the screens, including a top app bar and a bottom bar for the message input field.

## Design Principles

-   **Clean and Minimal**: The UI is designed to be clean and uncluttered, focusing on the core functionality of the IDE.
-   **Responsive**: The application is designed to be responsive and work well on a variety of screen sizes.
-   **Intuitive**: The navigation and workflow are designed to be intuitive and easy to understand for new users.

## A2A Integration

The A2A integration is a core part of the application, and it should be reflected in the UI. The following principles should be followed:

-   **Clear affordances for A2A actions**: The UI should make it clear to the user when they are interacting with the Jules API via the A2A protocol.
-   **Seamless integration**: The A2A integration should be seamless and not feel like a separate part of the application.
-   **Flexibility**: The UI should be flexible enough to support other AI providers in the future.
