# Miscellaneous

This document contains miscellaneous information about the Jules IDE project that does not fit into the other categories.

## Version Control

The project uses [JGit](https://www.eclipse.org/jgit/) for on-device version control operations. This allows the application to perform basic Git commands, such as `init`, `add`, `commit`, and `status`, without relying on an external Git client.

## Project Structure

The project is organized into the following main modules:

-   `app_android`: The main Android application module.
-   `jules-api-client`: A pure Kotlin module for interacting with the Jules API.

This modular structure helps to separate concerns and improve the maintainability of the codebase.