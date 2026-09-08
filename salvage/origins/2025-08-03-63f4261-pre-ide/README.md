# Geministrator

**An AI-powered development assistant built on a team of collaborative agents.**

---
## Getting Started

### Prerequisites

* Java 17+
* A valid Gemini API Key

### Installation

1. **Build the Installer:**
   Run the `jpackage` Gradle task. This will create a native installer in the `cli/build/installer/`
   directory.
    ```bash
    ./gradlew :cli:jpackage
    ```
2. **Install the Application:**
   Navigate to `cli/build/installer/` and run the generated installer (e.g.,
   `Geministrator-1.0.0.msi` on Windows).

3. **Run the application:**
   After installation, the application will be available on your system's PATH. You can run it from
   any terminal.

    ```bash
    # Run a workflow
    geministrator run "Your development task here"

    # Configure settings
    geministrator config -r
    ```

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.