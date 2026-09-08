# User Workflow

This document outlines the intended user workflow for the Jules IDE application.

1.  **First-Time Setup**:
    -   Upon launching the application for the first time, the user will be prompted to enter their Jules API key in the **Settings** screen.
    -   The user navigates to the Settings screen, enters their API key, and saves it.

2.  **Source Selection**:
    -   The user navigates to the **Source Selection** screen.
    -   The application fetches and displays a list of the user's available source repositories from the Jules API.

3.  **Session Creation**:
    -   The user selects a source repository from the list.
    -   A dialog appears, prompting the user to enter a title and an initial prompt for a new session.
    -   The user fills out the form and taps "Create".

4.  **Interacting with the Session**:
    -   Upon successful session creation, the application navigates to the **Session Screen**.
    -   The Session Screen displays the activity stream for the newly created session, including the initial prompt and any responses from the Jules agent.
    -   The user can send new messages to the agent using the message input bar at the bottom of the screen.

5.  **File Browsing and Editing**:
    -   The user can navigate to the **File Explorer** screen to browse the files within the selected project.
    -   Tapping on a file opens it in the **File Viewer Screen**, which uses the native Sora Editor for a rich code viewing experience.