# Google Agent Development Kit (ADK) Integration

This document provides a detailed overview of the Google Agent Development Kit (ADK) and its central role in the Geministrator project.

## The ADK as a Core Technology

The Google Agent Development Kit is a fundamental, non-negotiable component of the Geministrator architecture. It provides the core framework for creating, managing, and executing the AI agents that power the application's most advanced features.

**Under no circumstances should the ADK be removed, replaced, or refactored without a thorough understanding of its central role in the project.** All future agent-based features **must** be built on top of the ADK to ensure a consistent and stable architecture.

## The Role of the ADK in Geministrator

The ADK is used to:

-   **Define Agent Personas:** The ADK's `LlmAgent` builder provides a fluent API for defining the name, instruction, and description of our AI agents.
-   **Register Agent Tools:** The ADK's `addTool()` method allows us to provide our agents with the tools they need to interact with the outside world, for example by passing tool classes that wrap clients like the `GitHubApiClient`.
-   **Execute Agentic Workflows:** The ADK's `InMemoryRunner` provides a simple, efficient way to execute our agents and manage their lifecycle.

## Future Development

All future agent-based features, such as the planned central planner and researcher agents, must be implemented using the ADK. This will ensure that our agentic architecture remains consistent, maintainable, and scalable.

Any developer working on the Geministrator project should familiarize themselves with the ADK and its principles before attempting to implement any new agentic features.
