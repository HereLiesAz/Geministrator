# Geministrator Documentation

Welcome to the official documentation for the Geministrator project. This documentation is designed to provide a comprehensive overview of the project's architecture, features, and development conventions.

## Introduction

### Project Purpose

Geministrator is a mobile IDE (Integrated Development Environment) for the Jules API. The primary goal of the project is to provide a fully-featured development environment on mobile devices, leveraging the power of the Jules API for code generation, execution, and repository management.

A secondary, long-term goal is to integrate the Gemini API via the Google ADK to enable Agent-to-Agent (A2A) communication, allowing for complex, automated development workflows.

### Architecture Overview

The project follows a multi-module architecture, managed by Gradle. The main modules are:

-   **:app**: The main Android application module. The UI is built entirely with Jetpack Compose.
-   **:jules-api-client**: A pure Kotlin module responsible for all communication with the Jules API.
-   **:github-api-client**: A pure Kotlin module responsible for all communication with the GitHub API.

This modular design promotes separation of concerns, making the codebase easier to maintain, test, and scale.

### Core Architectural Principles

A fundamental, non-negotiable principle of the Geministrator project is its reliance on the **Google Agent Development Kit (ADK)**. The ADK is the backbone of the application's agentic features and is integral to its long-term vision.

**Under no circumstances should the ADK be removed, replaced, or refactored without a thorough understanding of its central role in the project.** All future agent-based features must be built on top of the ADK to ensure a consistent and stable architecture.

(Note: The ADK was temporarily removed during a build stabilization phase but is being re-integrated, as detailed in `TODO.md`.)

## Table of Contents

-   [**Screens** (`screens.md`)](screens.md): A detailed description of the application's user interface screens.
-   [**Workflow** (`workflow.md`)](workflow.md): An overview of the intended user workflow.
-   [**UI/UX** (`UI_UX.md`)](UI_UX.md): Details on the UI components and design principles.
-   [**Data Layer** (`data_layer.md`)](data_layer.md): A description of the application's data layer.
-   [**ADK Integration** (`adk_integration.md`)](adk_integration.md): Overview of the core agent framework.
-   [**Testing** (`testing.md`)](testing.md): Instructions for running the project's tests.
-   [**Task Flow** (`task_flow.md`)](task_flow.md): A high-level overview of the application's task flow.
-   [**Code of Conduct** (`conduct.md`)](conduct.md): Guidelines for contributing to the project.
-   [**Faux Pas (Common Pitfalls)** (`fauxpas.md`)](fauxpas.md): A list of common issues and how to resolve them.
-   [**Miscellaneous** (`misc.md`)](misc.md): Other relevant information.
