package io.kestra.plugin.terragrunt.cli;

import java.util.List;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.AbstractExecScript;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class TerragruntCLI extends AbstractExecScript implements RunnableTask<ScriptOutput> {
    private static final String DEFAULT_IMAGE = "alpine/terragrunt";

    @Builder.Default
    protected Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    @Schema(
        title = "Primary Terragrunt CLI commands",
        description = "Main commands run with `/bin/sh -c`, e.g., `terragrunt plan` or `terragrunt apply -auto-approve`."
    )
    @NotNull
    protected Property<List<String>> commands;

    @Override
    protected DockerOptions injectDefaults(RunContext runContext, DockerOptions original) throws IllegalVariableEvaluationException {
        var builder = original.toBuilder();
        if (original.getImage() == null) {
            builder.image(runContext.render(this.getContainerImage()).as(String.class).orElse(null));
        }
        return builder.build();
    }

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        return this.commands(runContext)
            .withInterpreter(interpreter)
            .withBeforeCommands(beforeCommands)
            .withBeforeCommandsWithOptions(true)
            .withCommands(commands)
            .run();
    }
}
