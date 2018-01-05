/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.language.internal;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.language.ComponentDependencies;
import org.gradle.language.ComponentWithDependencies;
import org.gradle.language.nativeplatform.ComponentWithObjectFiles;
import org.gradle.language.nativeplatform.internal.ComponentWithNames;
import org.gradle.language.nativeplatform.internal.Names;

import javax.inject.Inject;

public abstract class DefaultNativeBinary implements ComponentWithNames, ComponentWithObjectFiles, ComponentWithDependencies {
    private final String name;
    private final Names names;
    private final DirectoryProperty objectsDir;
    private final Configuration implementation;

    public DefaultNativeBinary(String name, ProjectLayout projectLayout, ConfigurationContainer configurations, Configuration componentImplementation) {
        this.name = name;
        this.names = Names.of(name);

        this.objectsDir = projectLayout.directoryProperty();

        this.implementation = configurations.create(name + "Implementation");
        implementation.setCanBeConsumed(false);
        implementation.setCanBeResolved(false);
        implementation.extendsFrom(componentImplementation);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Names getNames() {
        return names;
    }

    public DirectoryProperty getObjectsDir() {
        return objectsDir;
    }

    @Override
    public FileCollection getObjects() {
        return objectsDir.getAsFileTree().matching(new PatternSet().include("**/*.obj", "**/*.o"));
    }

    @Override
    public ComponentDependencies getDependencies() {
        return new ComponentDependencies() {
            @Override
            public void implementation(Object notation) {
                implementation.getDependencies().add(getDependencyHandler().create(notation));
            }
        };
    }

    @Override
    public void dependencies(Action<? super ComponentDependencies> action) {
        action.execute(getDependencies());
    }

    @Inject
    protected DependencyHandler getDependencyHandler() {
        throw new UnsupportedOperationException();
    }

    public Configuration getImplementationDependencies() {
        return implementation;
    }

}