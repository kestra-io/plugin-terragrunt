# Kestra Template Plugin

## What

- Provides plugin components under `io.kestra.plugin.terragrunt.cli`.
- Includes classes such as `TerragruntCLI`.

## Why

- This plugin integrates Kestra with Terragrunt CLI.
- It provides tasks that run Terragrunt CLI commands to init, plan, apply, and destroy infrastructure.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `templates`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.templates.Example`

### Project Structure

```
plugin-template/
├── src/main/java/io/kestra/plugin/templates/
├── src/test/java/io/kestra/plugin/templates/
├── build.gradle
└── README.md
```

## Local rules

- Base the wording on the implemented packages and classes, not on template README text.

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
