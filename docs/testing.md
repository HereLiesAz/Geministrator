# Testing

This document provides instructions for running the project's tests.

## Unit Tests

The project has unit tests in the `:app` module. To run them, use the following command from the
root of the project:

```bash
./gradlew :app:test
```

## Integration Tests

The project has integration tests for the `JulesApiClient` and `A2ACommunicator`. These tests are located in the `:app` module and can be run with the same command as the unit tests.
