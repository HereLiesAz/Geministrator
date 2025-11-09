# Data Layer

This document describes the application's data layer, which is responsible for handling all data-related operations, including communication with remote APIs and local data persistence.

## Remote Data Sources

### Jules API Client

The `:jules-api-client` module is a pure Kotlin library that provides a clean interface for interacting with the Jules API. It is used by the `A2ACommunicator` to perform all core IDE tasks.

### GitHub API Client

The `:github-api-client` module is a pure Kotlin library for interacting with the GitHub API. This is used by the Jules API for tasks like code review.

### A2A Communicator

The `A2ACommunicator` is a Hilt-injected service in the `:app` module. It wraps the `a2a-java-sdk-client` and manages all communication with the Jules API and other AI providers.

## Local Data Storage

The application uses Jetpack DataStore (via `SettingsRepository`) to persist API keys and other user settings locally on the device.
