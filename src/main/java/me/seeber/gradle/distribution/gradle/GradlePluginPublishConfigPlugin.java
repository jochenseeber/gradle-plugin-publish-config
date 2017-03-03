/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.seeber.gradle.distribution.gradle;

import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Model;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.internal.core.Hidden;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;

import com.google.common.base.Strings;
import com.gradle.publish.PluginBundleExtension;
import com.gradle.publish.PluginConfig;
import com.gradle.publish.PublishPlugin;

import me.seeber.gradle.distribution.gradle.GradlePluginPublishConfigPlugin.PluginRules.JavaPluginRules;
import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectConfig;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectContext;

/**
 * Configure project for plugin publishing
 */
public class GradlePluginPublishConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Provide the plugin bundle extension from the Plugin Publishing Plugin
         *
         * @param extensions Extension container to get extension
         * @return Plugin bundle extension
         */
        @Model
        @Hidden
        public PluginBundleExtension pluginBundleExtension(ExtensionContainer extensions) {
            return extensions.getByType(PluginBundleExtension.class);
        }

        /**
         * Initialize the plugin bundle extension
         *
         * @param bundleExtension Plugin bundle extension to initialize
         * @param projectConfig Project configuration
         * @param project Project context
         */
        @Defaults
        public void configureGradlePluginDevelopmentExtension(PluginBundleExtension bundleExtension,
                ProjectConfig projectConfig, ProjectContext project) {
            bundleExtension.setDescription(project.getDescription());
            bundleExtension.setWebsite(projectConfig.getWebsiteUrl());
            bundleExtension.setVcsUrl(projectConfig.getRepository().getWebsiteUrl());

            for (PluginConfig plugin : bundleExtension.getPlugins()) {
                if (Strings.isNullOrEmpty(plugin.getDisplayName())) {
                    plugin.setDisplayName(project.getDescription());
                }
            }
        }

        /**
         * Configure the eclipse task
         *
         * <ul>
         * <li>Make eclipse task depend on pluginUnterTestMetadata
         * </ul>
         *
         * @param task Task to configure
         * @param pluginExtension Plugin development extension
         * @param bundleExtension Plugin bundle extension
         */
        @Mutate
        public void configureTasks(@Each Task task, GradlePluginDevelopmentExtension pluginExtension,
                PluginBundleExtension bundleExtension) {
            if (task.getName().equals("eclipse")) {
                task.dependsOn("pluginUnderTestMetadata");
            }
        }

        /**
         * Rules for Java based plugins
         */
        public static class JavaPluginRules extends RuleSource {
            /**
             * Initialize the plugin development extension
             *
             * @param pluginExtension Plugin development extension
             */
            @Defaults
            public void initializeGradlePluginDevelopmentExtension(GradlePluginDevelopmentExtension pluginExtension) {
                pluginExtension.setAutomatedPublishing(false);
            }

        }
    }

    /**
     * <ul>
     * <li>Apply Project Config Plugin
     * <li>Apply Java Config Plugin
     * <li>Apply Groovy Config Plugin
     * <li>Apply Java Config Plugin
     * <li>Apply Java Gradle Plugin Plugin
     * <li>Apply Plugin Publishing Plugin
     * </ul>
     *
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    public void initialize() {
        getProject().getPluginManager().apply(ProjectConfigPlugin.class);
        getProject().getPluginManager().apply(PublishPlugin.class);

        getProject().getPluginManager().withPlugin("org.gradle.java-gradle-plugin", p -> {
            getProject().getPluginManager().apply(JavaPluginRules.class);
        });

        DependencyHandler dependencies = getProject().getDependencies();
        dependencies.add("compile", dependencies.gradleApi());
        dependencies.add("compile", dependencies.localGroovy());
        dependencies.add("testCompile", dependencies.gradleTestKit());
    }

}
