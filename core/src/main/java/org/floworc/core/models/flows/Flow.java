package org.floworc.core.models.flows;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.floworc.core.models.executions.TaskRun;
import org.floworc.core.models.tasks.ResolvedTask;
import org.floworc.core.models.tasks.Task;
import org.floworc.core.models.triggers.Trigger;
import org.floworc.core.runners.RunContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Value
@Builder
public class Flow {
    @NotNull
    private String id;

    public String uid() {
        return String.join("_", Arrays.asList(
            this.getNamespace(),
            this.getId(),
            this.getRevision() != null ? String.valueOf(this.getRevision()) : "-1"
        ));
    }

    @NotNull
    private String namespace;

    @With
    private Integer revision;

    @Valid
    private List<Input> inputs;

    @Valid
    private List<Task> tasks;

    @Valid
    private List<Task> errors;

    @Valid
    private List<Trigger> triggers;

    public Logger logger() {
        return LoggerFactory.getLogger("flow." + this.id);
    }

    public ResolvedTask findTaskByTaskRun(TaskRun taskRun, RunContext runContext) {
        return Stream.of(
            this.tasks,
            this.errors
        )
            .flatMap(tasks -> this.findTaskByTaskId(tasks, taskRun.getTaskId(), runContext, taskRun).stream())
            .map(task -> ResolvedTask.builder()
                .task(task)
                .parentId(taskRun.getParentTaskRunId())
                .value(taskRun.getValue())
                .build()
            )
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Can't find task with id '" + id + "' on flow '" + this.id + "'"));
    }

    private Optional<Task> findTaskByTaskId(List<Task> tasks, String id, RunContext runContext, TaskRun taskRun) {
        if (tasks == null) {
            return Optional.empty();
        }

        return tasks
            .stream()
            .flatMap(task -> task.findById(id, runContext, taskRun).stream())
            .findFirst();
    }
}
