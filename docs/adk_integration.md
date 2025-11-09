# A2A / Jules API Integration

This document provides a detailed overview of the Agent-to-Agent (A2A) protocol and its role in connecting Geministrator to the Jules API and other AI providers.

## The A2A SDK as the Core Technology

The **A2A Java SDK Client** is the absolute core of the Geministrator architecture. It is a non-negotiable component that provides the framework for communicating with various AI backends. The primary goal is to use the Jules API as the main AI provider, but the architecture is designed to be flexible enough to support other providers in the future.

This application is a **client** that consumes AI providers via the A2A protocol.

**Under no circumstances should the A2A SDK be removed.** All future agent-based features beyond the core Jules IDE **must** be built on top of the A2A SDK to ensure a consistent and stable architecture.

## The Role of the A2A SDK in Geministrator

The A2A SDK is used to:

-   **Define a Communicator:** A Hilt-injected `A2ACommunicator` class wraps the SDK client and integrates it with the `JulesRepository`.
-   **Route User Prompts:** The `SessionViewModel` routes all user prompts to the `A2ACommunicator`, which in turn uses the Jules API.
-   **Enable Specialized Agents:** This architecture allows the app to send tasks to the Jules API, which can be configured to act as a variety of specialized agents (e.g., "Planner Agent," "Code Review Agent").

## Future Development

All future agent-based features, such as the planned central planner and researcher agents, must be implemented by leveraging the Jules API via the `A2ACommunicator`. The architecture should also be open to integrating other AI providers in the future.
