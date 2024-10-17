/*
 * ResourceListClassTest.java
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.nio.file.Paths;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class ResourceListClassTest {

    @Test
    void testDefaultPublicVisibility() {
        ResourceListClass resourceListClass = new ResourceListClass();

        assertFalse(resourceListClass.publicVisibility);
    }

    @Test
    void testSetDefaultOutputDirectory() {
        ResourceListClass resourceListClass = new ResourceListClass();

        assertNull(resourceListClass.outputDirectory);

        Build build = mock(Build.class);
        when(build.getOutputDirectory()).thenReturn("target/classes");

        MavenProject project = mock(MavenProject.class);
        when(project.getBuild()).thenReturn(build);

        resourceListClass.setDefaultOutputDirectory(project);

        assertEquals(Paths.get("target/classes/generated-sources/resource-lists"), resourceListClass.outputDirectory);
    }

    @Test
    void testToString() {
        ResourceListClass resourceListClass = new ResourceListClass();
        resourceListClass.className = "test.ResourceList";
        resourceListClass.publicVisibility = true;
        resourceListClass.outputDirectory = Paths.get(URI.create("memory:/project/target/generated"));

        assertEquals("className: test.ResourceList, publicVisibility: true, outputDirectory: /project/target/generated",
                resourceListClass.toString());
    }
}
