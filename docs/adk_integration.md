# A2A / Remote ADK Integration

This document provides a detailed overview of the Agent-to-Agent (A2A) protocol and its role in connecting Geministrator to remote Google ADK agents.

## The A2A SDK as a Core Technology

The **A2A Java SDK Client** is a fundamental, non-negotiable component of the Geministrator architecture. It provides the core framework for communicating with remote, specialized AI agents built with the Google Agent Development Kit (ADK).

This application does **not** run the ADK locally. It is a **client** that consumes remote ADK agents via the A2A protocol.

**Under no circumstances should the A2A SDK be removed.** All future agent-based features beyond the core Jules IDE **must** be built on top of the A2A SDK to ensure a consistent and stable architecture.

## The Role of the A2A SDK in Geministrator

The A2A SDK is used to:

-   **Define a Communicator:** A Hilt-injected `A2ACommunicator` class wraps the SDK client.
-   **Route User Prompts:** The `SessionViewModel` is designed to check for specific commands (e.g., `/gemini`) and route those prompts to the `A2ACommunicator` instead of the Jules API.
-   **Enable Specialized Agents:** This architecture allows the app to send tasks to dedicated agents (e.g., "Planner Agent," "Code Review Agent") that have their own toolsets (like the `GitHubApiClient`) and are hosted remotely.

## Future Development

All future agent-based features, such as the planned central planner and researcher agents, must be implemented as remote ADK agents and consumed via the `A2ACommunicator`.