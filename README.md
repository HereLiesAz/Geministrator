# Geministrator

**An AI-powered development assistant built on a team of collaborative agents.**

Geministrator is a production-ready prototype that automates complex development workflows from within your IDE or command line. It uses a hierarchical council of six specialized AI agents to plan, research, execute, document, quality-check, and debug software development tasks.

Inspired by the script-based automation of projects like [**tmux-orchestrator**](https://github.com/Jedward23/Tmux-Orchestrator), this project reimagines workflow automation in the age of large language models, replacing static configuration files with a dynamic, resilient, and collaborative AI team.

---

## How It Works: The Agent Council

The Geministrator is built around a team of six specialized agents that collaborate to ensure high-quality, well-documented, and robust results.

1.  **The Orchestrator:** The team lead. It interprets the user's goals, directs the council, integrates the work of parallel managers, and makes the final decision to commit code.

2.  **The Architect:** The internal expert. It maintains a deep understanding of the project's codebase, structure, and history. It provides curated context to other agents and reviews all code for architectural compliance.

3.  **The Researcher:** The external expert. It connects to the outside world via tool access (e.g., web search) to find best practices, research libraries, and proactively scan for dependency vulnerabilities.

4.  **The Designer:** The project's scribe and historian. It is responsible for creating and maintaining the **Project Guidebook**—a set of living documents containing feature specifications, a changelog, and a "Document of Reasoning" to preserve institutional memory.

5.  **The Antagonist:** The designated "red teamer." Its sole purpose is to challenge the Orchestrator's plans *before* they are executed, searching for flaws, risks, or inefficiencies.

6.  **The Tech Support:** The error specialist. When a technical problem occurs that the agents cannot solve (like a Git merge conflict), Tech Support is called to analyze the error and propose a resolution strategy.

---

## Core Features & Principles

* **Concurrent & Isolated Work:** Complex tasks are broken down, and each sub-task is handled by a `Manager` agent on its own isolated Git branch, controlled by a user-configurable concurrency limit.
* **Mandatory Checkpoints:** All plans are reviewed by the Antagonist before execution, and all integrated code is reviewed by the Architect before being committed.
* **Self-Correction Loop:** The system automatically runs tests after code changes. If a test fails, it reads the error, and the Orchestrator attempts to generate a new workflow to fix the bug.
* **User-Controlled Commits:** A pre-commit review step (which can be disabled) ensures the user has the final say before code is written to history.
* **Session Persistence & Recovery:** Workflows are saved to a journal file, allowing the application to resume an incomplete task after a restart.
* **Graceful AI Restarts:** To prevent context overload, AI sessions are automatically summarized and restarted when they approach a user-defined token limit.

---

## Getting Started

### Prerequisites

* Java 17+
* Node.js and npm (for building the VS Code extension)
* A valid Gemini API Key

### 1. For the Command-Line (CLI) Version

1.  **Build the CLI:**
    ```bash
    ./gradlew :cli:installDist
    ```
2.  **Run the application:**
    * The first time you run it, it will prompt you for your Gemini API Key.
    * Use the `config` subcommand to manage settings.
    ```bash
    # Run a workflow
    ./cli/build/install/cli/bin/cli run "Your development task here"

    # Configure settings
    ./cli/build/install/cli/bin/cli config --toggle-review
    ./cli/build/install/cli/bin/cli config --set-concurrency=4
    ```

### 2. For the Android Studio Plugin

1.  **Build the Plugin:**
    ```bash
    ./gradlew :plugins:android-studio-plugin:buildPlugin
    ```
    This will generate a `.zip` file in `plugins/android-studio-plugin/build/distributions/`.

2.  **Install the Plugin:**
    * Open Android Studio > `Settings/Preferences` > `Plugins`.
    * Click the gear icon > `Install Plugin from Disk...` and select the `.zip` file.
    * Restart the IDE.

3.  **Run the Orchestrator:**
    * A "Welcome" wizard will appear on first run to guide you through setup.
    * Go to `Tools` > `Run Geministrator` to open the tool window.
    * Use the UI controls to configure settings like pre-commit review, concurrency, and token limits.

### 3. For the VS Code Extension

1.  **Build the CLI Prerequisite:** The VS Code extension depends on the CLI executable. You must build it first:
    ```bash
    ./gradlew :cli:installDist
    ```
2.  **Build the Extension:**
    ```bash
    cd plugins/vscode-extension
    npm install
    npm run compile
    ```
3.  **Run the Extension:**
    * Open the `Geministrator` project folder in VS Code.
    * Press `F5` to open a new "Extension Development Host" window with the extension running.
    * In the new window, open the Command Palette (`Ctrl+Shift+P` or `Cmd+Shift+P`) and type `Start Geministrator`.
    * This will open the Geministrator panel, where you can enter your prompt and run your workflows.