# Data Layer

This document describes the application's data layer, which is responsible for handling all data-related operations, including communication with the Jules API and local data persistence.

## Jules API Client

The `:jules-api-client` module is a pure Kotlin library that provides a clean interface for interacting with the Jules API. It is responsible for:

-   **Networking**: It uses the [Retrofit](https://square.github.io/retrofit/) library to make HTTP requests to the Jules API endpoints.
-   **JSON Parsing**: It uses the [`kotlinx.serialization`](https://github.com/Kotlin/kotlinx.serialization) library to parse the JSON responses from the API into Kotlin data classes.
-   **Authentication**: The `JulesApiClient` class takes an API key as a constructor parameter and includes it in the `X-Goog-Api-Key` header of every request.

### Key Components

-   **`JulesApiService`**: A Retrofit service interface that defines all the Jules API endpoints.
-   **Data Classes**: A set of Kotlin data classes that model the JSON responses from the Jules API (e.g., `Source`, `Session`, `Activity`).
-   **`JulesApiClient`**: The main client class that provides a high-level interface for making API calls.

## Local Data Storage

The application uses `AndroidConfigStorage` to persist the Jules API key and other user settings locally on the device. This class is responsible for:

-   **Saving the API Key**: It securely stores the user's Jules API key.
-   **Loading the API Key**: It retrieves the saved API key so it can be used by the `JulesApiClient`.
-   **Theme Preferences**: It also saves and loads the user's preferred application theme (Light, Dark, or System Default).