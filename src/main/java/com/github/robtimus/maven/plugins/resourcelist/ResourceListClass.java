/*
 * ResourceListClass.java
 * Copyright 2024 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.maven.plugins.resourcelist;

import java.io.File;
import java.nio.file.Path;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A class describing a class used to access resource lists.
 *
 * @author Rob Spoor
 */
public class ResourceListClass {

    /**
     * The fully qualified class name.
     *
     * @since 1.0
     */
    @Parameter(required = true)
    String className;

    /**
     * If {@code true}, classes and methods will have public visibility; otherwise they will be package private.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "false")
    boolean publicVisibility;

    Path outputDirectory;

    /**
     * The output directory where the resource list accessor class will be written to, without the package structure.
     * This directory will be added as a project source root if needed.
     *
     * @param outputDirectory The output directory where the resource list accessor class will be written to
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/resource-lists", required = true)
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory.toPath().toAbsolutePath().normalize();
    }
}
