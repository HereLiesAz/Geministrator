# Geministrator Documentation

Welcome to the official documentation for the Geministrator project. This documentation is designed to provide a comprehensive overview of the project's architecture, features, and development conventions.

## Introduction

### Project Purpose

Geministrator is a mobile IDE (Integrated Development Environment) for the Jules API. The primary goal of the project is to provide a fully-featured development environment on mobile devices, leveraging the power of the Jules API for code generation, execution, and repository management.

A secondary goal is to integrate the **A2A (Agent-to-Agent) protocol** to enable communication with remote, ADK-powered agents, allowing for complex, automated development workflows.

### Architecture Overview

The project follows a multi-module architecture, managed by Gradle. The main modules are:

-   **:app**: The main Android application module. The UI is built entirely with Jetpack Compose.
-   **:jules-api-client**: A pure Kotlin module responsible for all communication with the Jules API.
-   **:github-api-client**: A pure Kotlin module responsible for all communication with the GitHub API.

This modular design promotes separation of concerns, making the codebase easier to maintain, test, and scale.

### Core Architectural Principles

A fundamental, non-negotiable principle of the Geministrator project is its role as a **multi-agent client**. The application is designed to communicate with different agentic backends based on the user's needs.

-   **Jules API:** The primary backend for all in-session coding tasks.
-   **A2A Protocol:** The secondary backend for communicating with remote Google ADK agents for specialized, out-of-session tasks (e.g., code review, planning).

**Under no circumstances should the A2A client be removed.** All future agent-based features (beyond the core Jules IDE) must be built on top of the A2A-SDK to ensure a consistent and stable architecture.

## Table of Contents

-   [**Screens** (`screens.md`)](screens.md): A detailed description of the application's user interface screens.
-   [**Workflow** (`workflow.md`)](workflow.md): An overview of the intended user workflow.
-   [**UI/UX** (`UI_UX.md`)](UI_UX.md): Details on the UI components and design principles.
-   [**Data Layer** (`data_layer.md`)](data_layer.md): A description of the application's data layer.
-   [**A2A / ADK Integration** (`adk_integration.md`)](adk_integration.md): Overview of the remote agent framework.
-   [**Testing** (`testing.md`)](testing.md): Instructions for running the project's tests.
-   [**Task Flow** (`task_flow.md`)](task_flow.md): A high-level overview of the application's task flow.
-   [**Code of Conduct** (`conduct.md`)](conduct.md): Guidelines for contributing to the project.
-   [**Faux Pas (Common Pitfalls)** (`fauxpas.md`)](fauxpas.md): A list of common issues and how to resolve them.
-   [**Miscellaneous** (`misc.md`)](misc.md): Other relevant information.