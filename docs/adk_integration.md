# A2A / Remote ADK Integration

This document provides a detailed overview of the Agent-to-Agent (A2A) protocol and its role in connecting Geministrator to the Jules API.

## The A2A SDK as a Core Technology

The **A2A Java SDK Client** is a fundamental component of the Geministrator architecture. It provides the core framework for communicating with the Jules API, which acts as the AI backend for all agentic features.

This application is a **client** that consumes the Jules API via the A2A protocol.

**All future agent-based features beyond the core Jules IDE must be built on top of the A2A SDK to ensure a consistent and stable architecture.**

## The Role of the A2A SDK in Geministrator

The A2A SDK is used to:

-   **Define a Communicator:** A Hilt-injected `A2ACommunicator` class wraps the SDK client and integrates it with the `JulesRepository`.
-   **Route User Prompts:** The `SessionViewModel` routes all user prompts to the `A2ACommunicator`, which in turn uses the Jules API.
-   **Enable Specialized Agents:** This architecture allows the app to send tasks to the Jules API, which can be configured to act as a variety of specialized agents (e.g., "Planner Agent," "Code Review Agent").

## Future Development

All future agent-based features, such as the planned central planner and researcher agents, must be implemented by leveraging the Jules API via the `A2ACommunicator`.
