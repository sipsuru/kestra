package org.floworc.core.runners;

import org.floworc.core.models.executions.Execution;
import org.floworc.core.models.executions.TaskRun;
import org.floworc.core.models.flows.State;
import org.floworc.core.models.tasks.ResolvedTask;
import org.floworc.core.models.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FlowableUtils {
    public static List<TaskRun> resolveSequentialNexts(
        Execution execution,
        List<ResolvedTask> tasks,
        List<ResolvedTask> errors
    ) {
        return FlowableUtils.resolveSequentialNexts(execution, tasks, errors, null);
    }

    public static List<TaskRun> resolveSequentialNexts(
        Execution execution,
        List<ResolvedTask> tasks,
        List<ResolvedTask> errors,
        TaskRun parentTaskRun
    ) {
        List<ResolvedTask> currentTasks = execution.findTaskDependingFlowState(tasks, errors, parentTaskRun);

        // nothing
        if (currentTasks.size() == 0) {
            return new ArrayList<>();
        }

        // first one
        List<TaskRun> taskRuns = execution.findTaskRunByTasks(currentTasks, parentTaskRun);
        if (taskRuns.size() == 0) {
            return Collections.singletonList(currentTasks.get(0).toTaskRun(execution));
        }

        // first created, leave
        Optional<TaskRun> lastCreated = execution.findLastByState(currentTasks, State.Type.CREATED, parentTaskRun);
        if (lastCreated.isPresent()) {
            return new ArrayList<>();
        }

        // have running, leave
        Optional<TaskRun> lastRunning = execution.findLastByState(currentTasks, State.Type.RUNNING, parentTaskRun);
        if (lastRunning.isPresent()) {
            return new ArrayList<>();
        }

        // last success, find next
        Optional<TaskRun> lastTerminated = execution.findLastTerminated(currentTasks, parentTaskRun);
        if (lastTerminated.isPresent()) {
            int lastIndex = taskRuns.indexOf(lastTerminated.get());

            if (currentTasks.size() > lastIndex + 1) {
                return Collections.singletonList(currentTasks.get(lastIndex + 1).toTaskRun(execution));
            }
        }

        return new ArrayList<>();
    }

    public static Optional<State.Type> resolveState(
        Execution execution,
        List<ResolvedTask> tasks,
        List<ResolvedTask> errors,
        TaskRun parentTaskRun
    ) {
        List<ResolvedTask> currentTasks = execution.findTaskDependingFlowState(tasks, errors);

        if (currentTasks.size() > 0) {
            // handle nominal case, tasks or errors flow are ready to be analysed
            if (execution.isTerminated(currentTasks, parentTaskRun)) {
                return Optional.of(execution.hasFailed(currentTasks) ? State.Type.FAILED : State.Type.SUCCESS);
            }
        } else {
            // first call, the error flow is not ready, we need to notify the parent task that can be failed to init error flows
            if (execution.hasFailed(tasks, parentTaskRun)) {
                return Optional.of(execution.hasFailed(tasks) ? State.Type.FAILED : State.Type.SUCCESS);
            }
        }

        return Optional.empty();
    }

    public static List<ResolvedTask> resolveTasks(List<Task> tasks, TaskRun parentTaskRun) {
        if (tasks == null) {
            return null;
        }

        return tasks
            .stream()
            .map(task -> ResolvedTask.builder()
                .task(task)
                .parentId(parentTaskRun.getId())
                .build()
            )
            .collect(Collectors.toList());
    }
}
