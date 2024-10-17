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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.project.MavenProject;

/**
 * A class describing a class used to access resource lists.
 *
 * @author Rob Spoor
 */
public class ResourceListClass {

    /**
     * The fully qualified class name.
     */
    String className;

    /**
     * {@code true} if classes and methods should have public visibility, or {@code false} if they should be package private.
     */
    boolean publicVisibility = false;

    /**
     * The output directory where the resource list accessor class will be written to, without the package structure.
     */
    Path outputDirectory;

    void setDefaultOutputDirectory(MavenProject project) {
        outputDirectory = Paths.get(project.getBuild().getOutputDirectory()).resolve("generated-sources/resource-lists"); //$NON-NLS-1$
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return String.format("className: %s, publicVisibility: %b, outputDirectory: %s", className, publicVisibility, outputDirectory);
    }
}
