/*
 * ListResourcesMojoTest.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.github.robtimus.filesystems.memory.MemoryFileAttributeView;
import com.github.robtimus.filesystems.memory.MemoryFileSystemProvider;

@SuppressWarnings("nls")
class ListResourcesMojoTest {

    @Test
    void testSetResourceListFile() {
        ListResourcesMojo mojo = new ListResourcesMojo();
        mojo.setResourceListFile(new File("src/main/java/../../../target/classes/test.resources"));

        assertEquals(Paths.get("target/classes/test.resources").toAbsolutePath(), mojo.resourceListFile);
    }

    @Test
    void testSetResourceBaseDir() {
        ListResourcesMojo mojo = new ListResourcesMojo();
        mojo.setResourceBaseDir(new File("src/main/java/../../../target/classes/"));

        assertEquals(Paths.get("target/classes").toAbsolutePath(), mojo.resourceBaseDir);
    }

    @Nested
    class Execute {

        @BeforeEach
        void clearMemoryFileSystem() {
            MemoryFileSystemProvider.clear();
        }

        @Test
        void testResourceListFileIsDirectResource() throws MojoExecutionException, MojoFailureException, IOException {
            MavenProject project = mock(MavenProject.class);

            ListResourcesMojo mojo = new ListResourcesMojo();
            mojo.project = project;
            mojo.includes = new String[] { "**/*.properties" };
            mojo.setLog(mock(Log.class));

            mojo.resourceListFile = Paths.get(URI.create("memory:/project/target/classes/resources"));
            mojo.resourceBaseDir = Paths.get("src/main/resources");
            Path buildOutputDir = Paths.get(URI.create("memory:/project/target/classes"));

            mojo.execute(buildOutputDir);

            String resourceList = MemoryFileSystemProvider.getContentAsString(mojo.resourceListFile);
            assertEquals("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties\n", resourceList);

            ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
            verify(mojo.project).addResource(resourceCaptor.capture());

            Resource resource = resourceCaptor.getValue();
            assertEquals("/project/target/classes", resource.getDirectory());
            assertEquals(List.of("resources"), resource.getIncludes());
            assertEquals(List.of(), resource.getExcludes());
            assertEquals("", resource.getTargetPath());
        }

        @Test
        void testResourceListFileIsNestedResource() throws MojoExecutionException, MojoFailureException, IOException {
            MavenProject project = mock(MavenProject.class);

            ListResourcesMojo mojo = new ListResourcesMojo();
            mojo.project = project;
            mojo.includes = new String[] { "**/*.properties" };
            mojo.setLog(mock(Log.class));

            mojo.resourceListFile = Paths.get(URI.create("memory:/project/target/classes/subdir/resources"));
            mojo.resourceBaseDir = Paths.get("src/main/resources");
            Path buildOutputDir = Paths.get(URI.create("memory:/project/target/classes"));

            mojo.execute(buildOutputDir);

            String resourceList = MemoryFileSystemProvider.getContentAsString(mojo.resourceListFile);
            assertEquals("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties\n", resourceList);

            ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
            verify(mojo.project).addResource(resourceCaptor.capture());

            Resource resource = resourceCaptor.getValue();
            assertEquals("/project/target/classes/subdir", resource.getDirectory());
            assertEquals(List.of("resources"), resource.getIncludes());
            assertEquals(List.of(), resource.getExcludes());
            assertEquals("subdir", resource.getTargetPath());
        }

        @Test
        void testResourceListFileIsNoResource() throws MojoExecutionException, MojoFailureException, IOException {
            MavenProject project = mock(MavenProject.class);

            ListResourcesMojo mojo = new ListResourcesMojo();
            mojo.project = project;
            mojo.includes = new String[] { "**/*.properties" };
            mojo.setLog(mock(Log.class));

            mojo.resourceListFile = Paths.get(URI.create("memory:/project/target/subdir/resources"));
            mojo.resourceBaseDir = Paths.get("src/main/resources");
            Path buildOutputDir = Paths.get(URI.create("memory:/project/target/classes"));

            mojo.execute(buildOutputDir);

            String resourceList = MemoryFileSystemProvider.getContentAsString(mojo.resourceListFile);
            assertEquals("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties\n", resourceList);

            verify(mojo.project, never()).addResource(any());
        }

        @Test
        void testResourceListFileHasNoParent() throws MojoExecutionException, MojoFailureException, IOException {
            MavenProject project = mock(MavenProject.class);

            ListResourcesMojo mojo = new ListResourcesMojo();
            mojo.project = project;
            mojo.includes = new String[] { "**/*.properties" };
            mojo.setLog(mock(Log.class));

            mojo.resourceListFile = Paths.get(URI.create("memory:/project/target/subdir/resources")).getFileName();
            mojo.resourceBaseDir = Paths.get("src/main/resources");
            Path buildOutputDir = Paths.get(URI.create("memory:/project/target/classes"));

            mojo.execute(buildOutputDir);

            String resourceList = MemoryFileSystemProvider.getContentAsString(mojo.resourceListFile);
            assertEquals("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties\n", resourceList);

            verify(mojo.project, never()).addResource(any());
        }

        @Test
        void testResourceListWriteError() throws IOException {
            MavenProject project = mock(MavenProject.class);

            ListResourcesMojo mojo = new ListResourcesMojo();
            mojo.project = project;
            mojo.addDefaultExcludes = true;
            mojo.setLog(mock(Log.class));

            mojo.resourceListFile = Paths.get(URI.create("memory:/project/target/classes/subdir/resources"));
            mojo.resourceBaseDir = Paths.get("src/main/resources");
            Path buildOutputDir = Paths.get(URI.create("memory:/project/target/classes"));

            Files.createDirectories(mojo.resourceListFile.getParent());
            Files.getFileAttributeView(mojo.resourceListFile.getParent(), MemoryFileAttributeView.class).setReadOnly(true);

            MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute(buildOutputDir));

            AccessDeniedException cause = assertInstanceOf(AccessDeniedException.class, exception.getCause());
            assertEquals(mojo.resourceListFile.getParent().toString(), cause.getFile());

            assertFalse(Files.exists(mojo.resourceListFile));

            verify(mojo.project, never()).addResource(any());
        }

        @Nested
        class WithResourceListClass {

            @Test
            void testResourceListFileIsResource() throws MojoExecutionException, MojoFailureException, IOException {
                MavenProject project = mock(MavenProject.class);

                ListResourcesMojo mojo = new ListResourcesMojo();
                mojo.project = project;
                mojo.includes = new String[] { "**/*.properties" };
                mojo.setLog(mock(Log.class));

                mojo.resourceListFile = Paths.get(URI.create("memory:/project/target/classes/resources"));
                mojo.resourceBaseDir = Paths.get("src/main/resources");
                Path buildOutputDir = Paths.get(URI.create("memory:/project/target/classes"));

                mojo.resourceListClass = new ResourceListClass();
                mojo.resourceListClass.className = "com.github.robtimus.maven.plugins.resourcelist.ResourceList";
                mojo.resourceListClass.publicVisibility = false;
                mojo.resourceListClass.outputDirectory = Paths.get(URI.create("memory:/generated"));

                mojo.execute(buildOutputDir);

                String resourceList = MemoryFileSystemProvider.getContentAsString(mojo.resourceListFile);
                assertEquals("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties\n", resourceList);

                ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
                verify(mojo.project).addResource(resourceCaptor.capture());

                Resource resource = resourceCaptor.getValue();
                assertEquals("/project/target/classes", resource.getDirectory());
                assertEquals(List.of("resources"), resource.getIncludes());
                assertEquals(List.of(), resource.getExcludes());
                assertEquals("", resource.getTargetPath());

                Path resourceListClassFile = mojo.resourceListClass.outputDirectory
                        .resolve("com/github/robtimus/maven/plugins/resourcelist/ResourceList.java");

                assertTrue(Files.exists(resourceListClassFile));
            }

            @Test
            void testResourceListFileIsNoResource() throws IOException {
                MavenProject project = mock(MavenProject.class);

                ListResourcesMojo mojo = new ListResourcesMojo();
                mojo.project = project;
                mojo.includes = new String[] { "**/*.properties" };
                mojo.setLog(mock(Log.class));

                mojo.resourceListFile = Paths.get(URI.create("memory:/project/target/subdir/resources"));
                mojo.resourceBaseDir = Paths.get("src/main/resources");
                Path buildOutputDir = Paths.get(URI.create("memory:/project/target/classes"));

                mojo.resourceListClass = new ResourceListClass();
                mojo.resourceListClass.className = "com.github.robtimus.maven.plugins.resourcelist.ResourceList";
                mojo.resourceListClass.publicVisibility = false;
                mojo.resourceListClass.outputDirectory = Paths.get(URI.create("memory:/generated"));

                MojoFailureException exception = assertThrows(MojoFailureException.class, () -> mojo.execute(buildOutputDir));

                assertEquals(Messages.listResources.resourceListClassForNonResource(), exception.getMessage());

                String resourceList = MemoryFileSystemProvider.getContentAsString(mojo.resourceListFile);
                assertEquals("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties\n", resourceList);

                verify(mojo.project, never()).addResource(any());

                Path resourceListClassFile = mojo.resourceListClass.outputDirectory
                        .resolve("com/github/robtimus/maven/plugins/resourcelist/ResourceList.java");

                assertFalse(Files.exists(resourceListClassFile));
            }
        }
    }
}
