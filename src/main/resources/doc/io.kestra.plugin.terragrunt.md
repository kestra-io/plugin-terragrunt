# How to use the Terragrunt plugin

Run Terragrunt CLI commands — plan, apply, run-all, and more — from Kestra flows inside a container.

## Common properties

`containerImage` defaults to `alpine/terragrunt`. Pin a specific version by appending a tag. `taskRunner` controls where the container runs — defaults to Docker.

## Tasks

`cli.TerragruntCLI` runs one or more Terragrunt CLI commands set in `commands` (e.g. `terragrunt plan`, `terragrunt apply --auto-approve`, `terragrunt run-all apply`). Use `beforeCommands` for setup steps. Pass Terragrunt config files, variable files, or provider credentials via `inputFiles` or pull them from [namespace files](https://kestra.io/docs/concepts/namespace-files). Pass credentials and secrets as environment variables via `env` — store sensitive values in [secrets](https://kestra.io/docs/concepts/secret). Apply runner properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).
