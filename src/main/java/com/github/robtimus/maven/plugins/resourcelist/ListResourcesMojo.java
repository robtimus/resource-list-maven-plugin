/*
 * ListResourcesMojo.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Creates a list of resources in the current project.
 *
 * @author Rob Spoor
 */
@Mojo(name = "list-resources", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true, threadSafe = true)
public class ListResourcesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    RepositorySystemSession repositorySystemSession;

    Path resourceListFile;
    Path resourceBaseDir;

    /**
     * The patterns that define which resources to include.
     *
     * @since 1.0
     */
    @Parameter
    String[] includes;

    /**
     * The patterns that define which resources to exclude.
     *
     * @since 1.0
     */
    @Parameter
    String[] excludes;

    /**
     * Whether to add some default exclude patterns. Set this parameter to {@code false} to not add the following exclude patterns:
     * <ul>
     *   <li>{@code META-INF/MANIFEST.MF}</li>
     *   <li>{@code META-INF/LICENSE.*}</li>
     *   <li>{@code META-INF/services/**}</li>
     *   <li>{@code META-INF/maven/**}</li>
     *   <li>{@code META-INF/m2e/**}</li>
     * </ul>
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "true")
    boolean addDefaultExcludes;

    /**
     * If given a class will be generated that can be used to access the resource list.
     * This class will have two static methods:
     * <ul>
     *   <li>{@code absolute()} will return an instance of the class that returns absolute paths to the resources.
     *       These paths can be used with methods like `Class.getResource`.</li>
     *   <li>{@code relative()} will return an instance of the class that returns paths to the resources relative to the root folder.
     *       These paths can be used with methods like `ClassLoader.getResource`.</li>
     * </ul>
     * <p>
     * This class will also have three non-static methods:
     * <ul>
     *   <li>{@code Stream<String> stream()} will return a stream over the resources. This stream should be closed after use.</li>
     *   <li>{@code List<String> list()} will return a list containing the resources.</li>
     *   <li>{@code void forEach(Consumer<? super String> action)} will run an action for each resource.</li>
     * </ul>
     * <p>
     * The following nested properties can be set:
     * <ul>
     *   <li>{@code className} (required): the fully qualified class name of the accessor class.</li>
     *   <li>{@code publicVisibility}: {@code true} if classes and methods should have public visibility, or {@code false} (default) if they should
     *       be package private.</li>
     *   <li>{@code outputDirectory}: the output directory where the resource list accessor class will be written to, without the package structure.
     *       This directory will be added as a project source root.
     *       Defaults to <code>${project.build.outputDirectory}/generated-sources/resource-lists</code>.</li>
     * </ul>
     * <p>
     * It's an error to specify the resource list class in combination with a resource list file that is not located in the project build's output
     * directory (usually {@code target/classes}).
     *
     * @since 1.0
     */
    @Parameter
    ResourceListClass resourceListClass;

    /**
     * The file to store the resource list in.
     *
     * @param resourceListFile The file to store the resource list in.
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/${project.artifactId}.resources", required = true)
    public void setResourceListFile(File resourceListFile) {
        this.resourceListFile = resourceListFile.toPath().toAbsolutePath().normalize();
    }

    /**
     * The resource base directory.
     *
     * @param resourceBaseDir The resource base directory.
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    public void setResourceBaseDir(File resourceBaseDir) {
        this.resourceBaseDir = resourceBaseDir.toPath().toAbsolutePath().normalize();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path buildOutputDir = Paths.get(project.getBuild().getOutputDirectory()).toAbsolutePath().normalize();
        execute(buildOutputDir);
    }

    void execute(Path buildOutputDir) throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        ResourcesScanner scanner = new ResourcesScanner(includes, excludes, addDefaultExcludes, log);
        // Exclude the resource list file itself
        scanner.excludeFile(resourceListFile);
        Set<String> resources = scanner.scan(resourceBaseDir);

        log.info(Messages.listResources.writingListFile(resourceListFile));
        writeResources(resources);

        Resource resourceListResource = resourceListResource(buildOutputDir);
        if (resourceListResource != null) {
            project.addResource(resourceListResource);
            log.debug(Messages.listResources.addedListFileResource(resourceListFile));
        }

        if (resourceListClass != null) {
            if (resourceListResource == null) {
                throw new MojoFailureException(Messages.listResources.resourceListClassForNonResource());
            }
            if (resourceListClass.outputDirectory == null) {
                resourceListClass.setDefaultOutputDirectory(project);
                log.debug(Messages.listResources.setDefaultResourceListClassOutputDirectory(resourceListClass.outputDirectory));
            }
            ResourceListClassGenerator generator = new ResourceListClassGenerator();
            generator.generateClassFile(resourceListClass, resourceListResource);

            addSourceRoot(project.getCompileSourceRoots(), project::addCompileSourceRoot, resourceListClass.outputDirectory);
            log.debug(Messages.listResources.addedResourceListClassOutputDirectory(resourceListClass.outputDirectory));
        }
    }

    private void writeResources(Set<String> resources) throws MojoExecutionException {
        Path targetFile = resourceListFile;
        Path parentDirectory = targetFile.getParent();
        try {
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(targetFile)) {
                for (String resource : resources) {
                    writer.append(resource).append('\n');
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Resource resourceListResource(Path buildOutputDir) {
        if (resourceListFile.startsWith(buildOutputDir)) {
            Path relativePath = buildOutputDir.relativize(resourceListFile);
            Path resourcePath = relativePath.getParent();

            Resource result = new Resource();
            // resourceListFile has a parent; either project.build.outputDirectory itself or a sub directory
            result.setDirectory(resourceListFile.getParent().toString());
            result.addInclude(resourceListFile.getFileName().toString());
            result.setTargetPath(resourcePath == null ? "" : resourcePath.toString()); //$NON-NLS-1$
            return result;
        }
        return null;
    }

    private void addSourceRoot(List<String> sourceRoots, Consumer<String> sourceRootAdder, Path sourceRoot) {
        String newSourcePath = sourceRoot.toString();
        String newAbsoluteSourcePath = sourceRoot.toAbsolutePath().normalize().toString();

        if (!sourceRoots.contains(newSourcePath) && !sourceRoots.contains(newAbsoluteSourcePath)) {
            sourceRootAdder.accept(newAbsoluteSourcePath);
        }
    }
}
