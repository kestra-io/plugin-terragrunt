package io.kestra.plugin.terragrunt.cli;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class TerragruntCLITest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        var environmentKey = "MY_KEY";
        var environmentValue = "MY_VALUE";

        var builder = TerragruntCLI.builder()
            .id(IdUtils.create())
            .type(TerragruntCLI.class.getName())
            .commands(Property.ofValue(List.of("terragrunt --version")));

        var runner = builder.build();

        var runContext = TestsUtils.mockRunContext(runContextFactory, runner, Map.of(
            "environmentKey", environmentKey,
            "environmentValue", environmentValue
        ));

        ScriptOutput scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));

        runner = builder
            .env(Property.ofValue(Map.of(environmentKey, environmentValue)))
            .commands(
                Property.ofValue(
                    List.of(
                        "echo \"::{\\\"outputs\\\":{" +
                            "\\\"customEnv\\\":\\\"$" + environmentKey + "\\\"" +
                            "}}::\""
                    )
                )
            )
            .build();

        scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));
        assertThat(scriptOutput.getVars().get("customEnv"), is(environmentValue));
    }
}
