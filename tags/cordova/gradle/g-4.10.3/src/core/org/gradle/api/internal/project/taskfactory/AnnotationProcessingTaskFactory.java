/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.project.taskfactory;

import org.gradle.api.Task;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.internal.reflect.Instantiator;

/**
 * A {@link ITaskFactory} which determines task actions, inputs and outputs based on annotation attached to the task properties. Also provides some validation based on these annotations.
 */
public class AnnotationProcessingTaskFactory implements ITaskFactory {
    private final TaskClassInfoStore taskClassInfoStore;
    private final ITaskFactory taskFactory;

    public AnnotationProcessingTaskFactory(TaskClassInfoStore taskClassInfoStore, ITaskFactory taskFactory) {
        this.taskClassInfoStore = taskClassInfoStore;
        this.taskFactory = taskFactory;
    }

    @Override
    public ITaskFactory createChild(ProjectInternal project, Instantiator instantiator) {
        return new AnnotationProcessingTaskFactory(taskClassInfoStore, taskFactory.createChild(project, instantiator));
    }

    @Override
    public <S extends Task> S create(String name, Class<S> type) {
        return process(taskFactory.create(name, type));
    }


    @Override
    public <S extends Task> S create(TaskIdentity<S> taskIdentity, Object... args) {
        return process(taskFactory.create(taskIdentity, args));
    }

    private <S extends Task> S process(S task) {
        TaskClassInfo taskClassInfo = taskClassInfoStore.getTaskClassInfo(task.getClass());

        if (taskClassInfo.isIncremental()) {
            // Add a dummy upToDateWhen spec: this will force TaskOutputs.hasOutputs() to be true.
            task.getOutputs().upToDateWhen(new Spec<Task>() {
                public boolean isSatisfiedBy(Task element) {
                    return true;
                }
            });
        }

        for (TaskActionFactory actionFactory : taskClassInfo.getTaskActionFactories()) {
            ((TaskInternal) task).prependParallelSafeAction(actionFactory.create());
        }

        // Enabled caching if task type is annotated with @CacheableTask
        if (taskClassInfo.isCacheable()) {
            task.getOutputs().cacheIf("Annotated with @CacheableTask", Specs.SATISFIES_ALL);
        }

        return task;
    }
}