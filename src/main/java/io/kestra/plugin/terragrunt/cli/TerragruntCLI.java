package io.kestra.plugin.terragrunt.cli;

import java.util.*;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.*;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.kestra.plugin.scripts.runner.docker.Docker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Run Terragrunt CLI commands in Docker",
    description = "Executes Terragrunt commands inside the task runner container. Defaults to the `alpine/terragrunt` image and assumes a remote state backend such as S3, GCS, or Terraform Cloud."
)
@Plugin(
    examples = {
        @Example(
            title = "Initialize Terragrunt, then create and apply the plan",
            full = true,
            code = """
                id: git_terragrunt
                namespace: company.team

                tasks:
                  - id: git
                    type: io.kestra.plugin.core.flow.WorkingDirectory
                    tasks:
                      - id: clone_repository
                        type: io.kestra.plugin.git.Clone
                        url: https://github.com/anna-geller/kestra-ci-cd
                        branch: main

                      - id: terragrunt
                        type: io.kestra.plugin.terragrunt.cli.TerragruntCLI
                        beforeCommands:
                          - terragrunt init
                        inputFiles:
                          terraform.tfvars: |
                            username            = "cicd"
                            password            = "{{ secret('CI_CD_PASSWORD') }}"
                            hostname            = "https://demo.kestra.io"
                        outputFiles:
                          - "*.txt"
                        commands:
                          - terragrunt plan 2>&1 | tee plan_output.txt
                          - terragrunt apply -auto-approve 2>&1 | tee apply_output.txt
                        env:
                          AWS_ACCESS_KEY_ID: "{{ secret('AWS_ACCESS_KEY_ID') }}"
                          AWS_SECRET_ACCESS_KEY: "{{ secret('AWS_SECRET_ACCESS_KEY') }}"
                          AWS_DEFAULT_REGION: "{{ secret('AWS_DEFAULT_REGION') }}"
                """
        ),
        @Example(
            title = "Pin Terragrunt version and run validate then plan",
            full = true,
            code = """
                id: terragrunt_plan_only
                namespace: company.team

                tasks:
                  - id: terragrunt
                    type: io.kestra.plugin.terragrunt.cli.TerragruntCLI
                    containerImage: alpine/terragrunt:1.10.3
                    beforeCommands:
                      - terragrunt init -input=false
                    commands:
                      - terragrunt validate -no-color
                      - terragrunt plan -input=false -no-color -out=tfplan
                    env:
                      TF_VAR_region: us-east-1
                    outputFiles:
                      - tfplan
                """
        )
    }
)
public class TerragruntCLI extends Task implements RunnableTask<ScriptOutput>, NamespaceFilesInterface, InputFilesInterface, OutputFilesInterface {
    private static final String DEFAULT_IMAGE = "alpine/terragrunt";

    @Schema(
        title = "Pre-run setup commands",
        description = "Commands executed before `commands`, typically `terragrunt init`."
    )
    protected Property<List<String>> beforeCommands;

    @Schema(
        title = "Primary Terragrunt CLI commands",
        description = "Main commands run with `/bin/sh -c`, e.g., `terragrunt plan` or `terragrunt apply -auto-approve`."
    )
    @NotNull
    protected Property<List<String>> commands;

    @Schema(
        title = "Environment variables for commands",
        description = "Extra variables passed to the process; use for provider credentials and configuration."
    )
    @PluginProperty(
        additionalProperties = String.class,
        dynamic = true
    )
    protected Map<String, String> env;

    @Schema(
        title = "The task runner to use.",
        description = "Task runners are provided by plugins, each have their own properties."
    )
    @PluginProperty
    @Builder.Default
    @Valid
    private TaskRunner<?> taskRunner = Docker.instance();

    @Schema(
        title = "Task runner container image",
        description = "Used only when the task runner is container-based; defaults to `alpine/terragrunt`."
    )
    @Builder.Default
    private Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    private NamespaceFiles namespaceFiles;

    private Object inputFiles;

    private Property<List<String>> outputFiles;

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        var renderedOutputFiles = runContext.render(this.outputFiles).asList(String.class);
        return new CommandsWrapper(runContext)
            .withWarningOnStdErr(true)
            .withTaskRunner(this.taskRunner)
            .withContainerImage(runContext.render(this.containerImage).as(String.class).orElse(null))
            .withEnv(Optional.ofNullable(this.env != null ? runContext.renderMap(this.env) : null).orElse(new HashMap<>()))
            .withNamespaceFiles(namespaceFiles)
            .withInputFiles(inputFiles)
            .withOutputFiles(renderedOutputFiles.isEmpty() ? null : renderedOutputFiles)
            .withInterpreter(Property.ofValue(List.of("/bin/sh", "-c")))
            .withBeforeCommands(this.beforeCommands)
            .withCommands(this.commands)
            .run();
    }
}
