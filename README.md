# Geministrator

**An AI-powered development assistant built on a team of collaborative agents.**

---

## 🤖 Key Capabilities

- **Autonomous Workflows**: Deconstruct high-level tasks (e.g., "add user authentication") into a
  detailed, multi-step execution plan.
- **Agent-Based Architecture**: A council of specialized AI agents (Architect, Researcher,
  Antagonist) collaborate to plan, execute, and self-correct development tasks.
- **Intelligent Task Triage**: Automatically determines which specialists are needed for a given
  task, conserving resources and tokens for simple requests.
- **Persistent & Resumable Sessions**: Workflows are saved automatically. If a session is paused,
  fails, or requires user input, it can be resumed seamlessly.
- **Git Integration**: Automatically creates feature branches, commits work-in-progress, and
  integrates completed tasks back into the main branch.
- **Configurable & Malleable**: Agent behavior is defined in external JSON, allowing you to
  customize their core instructions without recompiling.
- **Dual Authentication**: Supports both Google Cloud's Application Default Credentials (ADC) for
  potential free-tier access and traditional API keys.
- **Free-Tier Mode**: Can be configured to exclusively use free-tier models and ADC authentication
  to avoid unexpected costs.

---

## 🏗️ Architecture: The Council of Agents

Geministrator operates not as a single monolithic AI, but as a team of specialists with distinct
roles, managed by a central Orchestrator. This approach allows for more robust and nuanced
problem-solving.

```
┌──────────────────┐
│   Orchestrator   │ ← You interact here. Manages the master plan.
└────────┬─────────┘
         │ Deploys agents based on a triage assessment
         │
  ┌──────┴───────────┐
 F│      Manager     │ ← Executes the step-by-step workflow for a single task
 E├──────────────────┤
 A│    Architect     │ ← Analyzes existing code to provide context
 T├──────────────────┤
 U│    Researcher    │ ← Scours the web for best practices and documentation
 R├──────────────────┤
 E│     Designer     │ ← Creates specifications and updates changelogs
  ├──────────────────┤
 B│    Antagonist    │ ← Critiques plans to find flaws before execution
 R├──────────────────┤
 A│   Tech Support   │ ← Analyzes merge conflicts and other technical failures
 N└──────────────────┘
 C         │
 H         ▼
┌──────────────────┐
│ ExecutionAdapter │ ← Interacts with the shell, file system, and Git
└──────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

* **Java 21+**: Ensure a JDK is installed and the `JAVA_HOME` environment variable is set.
* **(Optional) Google Cloud SDK**: For ADC authentication (recommended), install and configure
  `gcloud`. Run `gcloud auth application-default login` to set up your credentials.

### Installation

1. **Build the Installer:**
   From the root of the project, run the `jpackage` Gradle task. This will create a native installer
   in the `cli/build/installer/` directory.
    ```bash
    ./gradlew :cli:jpackage
    ```

2. **Install the Application:**
   Navigate to `cli/build/installer/` and run the generated installer (e.g.,
   `Geministrator-1.0.0.msi` on Windows or the equivalent `.deb`/`.rpm`/`.pkg` on other systems).

3. **Run from Anywhere:**
   After installation, the `geministrator` command will be available on your system's PATH. You can
   run it from any terminal.

---

## ⚙️ Configuration

The first time you run the application, it will create a configuration directory at
`~/.gemini-orchestrator/`. You can manage settings via the `config` command.

### Authentication: ADC vs. API Key

Geministrator supports two authentication methods. By default, it will try to use **Application
Default Credentials (ADC)** by shelling out to `gcloud`. If this fails, it will fall back to using a
standard **API Key**.

* **To set your preferred method:**
  ```bash
  # Prioritize gcloud authentication (default)
  geministrator config --auth-method adc

  # Use a manually provided API key
  geministrator config --auth-method apikey
  ```

### Completely Free Tier Mode

You can enforce the use of free-tier models and ADC authentication to prevent accidental charges. If
ADC is not configured, Geministrator will refuse to run in this mode.

* **To enable or disable free tier mode:**
  ```bash
  geministrator config --free-tier true
  geministrator config --free-tier false
  ```

### Other Settings

You can configure other operational parameters:

```bash
# Toggle the final user review before committing merged code
geministrator config -r

# Set the number of sub-tasks to run in parallel
geministrator config -c 4

# Set a custom token limit for the AI conversation history
geministrator config -t 700000

# Configure web search credentials (for the Researcher agent)
geministrator config --search-api-key "YOUR_Google Search_API_KEY"
geministrator config --search-engine-id "YOUR_PROGRAMMABLE_SEARCH_ENGINE_ID"
```

---

## 💻 Usage

### Running a Workflow

The primary command is `run`. It takes a high-level prompt describing your development task.

```bash
# Run a simple task
geministrator run "Refactor the UserService class to use the new DatabaseWrapper."

# Run a more complex task with a formal specification file
geministrator run "Implement the user profile page" --spec-file "docs/specs/profile_page.md"
```

### Writing a Project Specification

For complex tasks, providing a spec file (inspired
by [Tmux-Orchestrator](https://github.com/a-s-w/tmux-orchestrator)) helps the AI generate a more
accurate master plan.

**Example `profile_page.md`:**
`markdown
PROJECT: My Web App
GOAL: Create a new user profile page.

CONSTRAINTS:

- Use the existing React component library.
- Follow current code patterns for state management.
- The page must be responsive.

DELIVERABLES:

1. A new route at `/profile`.
2. A component to display user information (name, email).
3. A form to update the user's password.
4. All new code must have corresponding unit tests.
   `

### Resuming a Session

If a workflow is paused (e.g., the AI needs clarification) or fails, the session state is saved to
`.orchestrator/session.json`. Simply re-running the original `geministrator run` command in the same
directory will prompt you to resume the workflow from where it left off.

---

## 🔧 Customizing Agent Behavior

The core instructions and "personality" of each AI agent are not hardcoded. They are stored in a
plain text JSON file located at `~/.gemini-orchestrator/prompts.json`. You can edit this file to
change how the agents behave.

For example, you could make the `Antagonist` more critical or instruct the `Architect` to prefer a
different coding style.

* **To reset all prompts to their original defaults:**
  ```bash
  geministrator config --reset-prompts true
  ```
  This will delete your custom `prompts.json`, and the application will use the default prompts from
  its internal resources on the next run.

---

## 📜 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.