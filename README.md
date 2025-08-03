# Geministrator

**An AI-powered development assistant built on a team of collaborative agents.**

Geministrator is a standalone tool that automates complex development workflows from your command
line.

---
## Getting Started

### Prerequisites

* Java 17+
* A valid Gemini API Key

### Installation and Usage

1.  **Build the CLI:**
    ```bash
    ./gradlew installDist
    ```
2.  **Run the application:**
    * The first time you run it, it will prompt you for your Gemini API Key.
    * Use the `config` subcommand to manage settings.

    ```bash
    # Run a workflow
    ./build/dist/bin/geministrator run "Your development task here"

    # Configure settings
    ./build/dist/bin/geministrator config -r
    ```