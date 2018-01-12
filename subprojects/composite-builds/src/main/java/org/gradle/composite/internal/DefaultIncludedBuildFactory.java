/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.composite.internal;

import org.gradle.StartParameter;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.internal.BuildDefinition;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.initialization.GradleLauncher;
import org.gradle.initialization.NestedBuildFactory;
import org.gradle.initialization.layout.BuildLayoutFactory;
import org.gradle.internal.Factory;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.work.WorkerLeaseService;
import org.gradle.plugin.management.internal.PluginRequests;

import java.io.File;

public class DefaultIncludedBuildFactory implements IncludedBuildFactory {
    private final Instantiator instantiator;
    private final StartParameter rootBuildStartParameter;
    private final WorkerLeaseService workerLeaseService;
    private final BuildLayoutFactory buildLayoutFactory;

    public DefaultIncludedBuildFactory(Instantiator instantiator, StartParameter rootBuildStartParameter, WorkerLeaseService workerLeaseService, BuildLayoutFactory buildLayoutFactory) {
        this.instantiator = instantiator;
        this.rootBuildStartParameter = rootBuildStartParameter;
        this.workerLeaseService = workerLeaseService;
        this.buildLayoutFactory = buildLayoutFactory;
    }

    private void validateBuildDirectory(File dir) {
        if (!dir.exists()) {
            throw new InvalidUserDataException(String.format("Included build '%s' does not exist.", dir));
        }
        if (!dir.isDirectory()) {
            throw new InvalidUserDataException(String.format("Included build '%s' is not a directory.", dir));
        }
    }

    private void validateIncludedBuild(IncludedBuild includedBuild, SettingsInternal settings) {
        File settingsFile = buildLayoutFactory.findExistingSettingsFileIn(settings.getSettingsDir());
        if (settingsFile == null) {
            throw new InvalidUserDataException(String.format("Included build '%s' must have a settings file.", includedBuild.getName()));
        }
    }

    @Override
    public IncludedBuildInternal createBuild(File buildDirectory, PluginRequests pluginRequests, NestedBuildFactory nestedBuildFactory) {
        validateBuildDirectory(buildDirectory);
        Factory<GradleLauncher> factory = new ContextualGradleLauncherFactory(buildDirectory, pluginRequests, nestedBuildFactory);
        DefaultIncludedBuild includedBuild = instantiator.newInstance(DefaultIncludedBuild.class, buildDirectory, factory, workerLeaseService.getCurrentWorkerLease());

        SettingsInternal settingsInternal = includedBuild.getLoadedSettings();
        validateIncludedBuild(includedBuild, settingsInternal);
        return includedBuild;
    }

    private class ContextualGradleLauncherFactory implements Factory<GradleLauncher> {
        private final File buildDirectory;
        private final PluginRequests pluginRequests;
        private final NestedBuildFactory nestedBuildFactory;

        public ContextualGradleLauncherFactory(File buildDirectory, PluginRequests pluginRequests, NestedBuildFactory nestedBuildFactory) {
            this.buildDirectory = buildDirectory;
            this.pluginRequests = pluginRequests;
            this.nestedBuildFactory = nestedBuildFactory;
        }

        @Override
        public GradleLauncher create() {
            BuildDefinition includedBuildDefinition = createBuildDefinition();
            GradleLauncher gradleLauncher = nestedBuildFactory.nestedInstance(includedBuildDefinition);
            return gradleLauncher;
        }

        private BuildDefinition createBuildDefinition() {
            StartParameter includedBuildStartParam = rootBuildStartParameter.newBuild();
            includedBuildStartParam.setProjectDir(buildDirectory);
            includedBuildStartParam.setSearchUpwards(false);
            includedBuildStartParam.setConfigureOnDemand(false);
            includedBuildStartParam.setInitScripts(rootBuildStartParameter.getInitScripts());
            return new BuildDefinition(includedBuildStartParam, pluginRequests);
        }
    }
}
