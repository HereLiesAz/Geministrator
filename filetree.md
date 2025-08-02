gemini-orchestrator/
├── .gitignore
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
│
├── 📁 common/
│   └── src/main/kotlin/com/gemini/orchestrator/common/
│       ├── AbstractCommand.kt
│       └── ExecutionResult.kt
│
├── 📁 core/
│   └── src/main/kotlin/com/gemini/orchestrator/core/
│       ├── config/
│       │   └── ConfigStorage.kt
│       ├── council/
│       │   ├── AllAgents.kt
│       │   ├── ILogger.kt
│       │   └── TechSupport.kt
│       ├── tokenizer/
│       │   └── Tokenizer.kt
│       ├── GeminiService.kt
│       ├── Manager.kt
│       └── Orchestrator.kt
│
├── 📁 adapters/
│   ├── 📁 adapter-cli/
│   │   └── src/main/kotlin/com/gemini/orchestrator/adapter/cli/
│   │       ├── CliAdapter.kt
│   │       └── CliConfigStorage.kt
│   │
│   └── 📁 adapter-android-studio/
│       └── src/main/kotlin/com/gemini/orchestrator/adapter/as/
│           ├── AndroidStudioAdapter.kt
│           └── PluginConfigStorage.kt
│
└── 📁 products/
    ├── 📁 cli/
    │   └── src/main/kotlin/com/gemini/orchestrator/cli/
    │       └── Main.kt
    │
    ├── 📁 android-studio-plugin/
    │   ├── src/main/kotlin/com/gemini/orchestrator/plugin/
    │   │   ├── OrchestratorToolWindowFactory.kt
    │   │   ├── ProgressLogger.kt
    │   │   └── RunOrchestratorAction.kt
    │   └── src/main/resources/META-INF/
    │       └── plugin.xml
    │
    └── 📁 vscode-extension/
        ├── .vscodeignore
        ├── package.json
        ├── tsconfig.json
        └── 📁 src/
            └── extension.ts
