# Task Flow

This document provides a high-level overview of the application's task flow, from initial setup to interacting with the Jules agent.

1.  **Onboarding**:
    -   The user launches the app for the first time.
    -   The user is directed to the **Settings** screen to enter their Jules API key.

2.  **Core Loop**:
    -   The user selects a source repository from the **Source Selection** screen.
    -   The user creates a new session by providing a title and an initial prompt.
    -   The user interacts with the Jules agent in the **Session Screen**, sending messages and reviewing the agent's responses and activities.
    -   The user can also send messages to a remote ADK agent via the `/gemini` command.

3.  **Code Exploration**:
    -   The user can browse the project's file structure (via the Jules API).
    -   Tapping a file in the activity stream opens it in the native **IDE Screen**.
    -   The user can edit, run, and commit code from the **IDE Screen**.