/*
 * ResourcesScannerTest.java
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@SuppressWarnings("nls")
class ResourcesScannerTest {

    @Nested
    class Scan {

        @Test
        void testDefaultIncludesAndExcludes() throws MojoExecutionException {
            Log log = mock(Log.class);
            ResourcesScanner scanner = new ResourcesScanner(null, null, true, log);
            Path baseDir = Paths.get("src/main/resources");

            Set<String> resources = scanner.scan(baseDir);

            assertThat(resources, contains(
                    "com/github/robtimus/maven/plugins/resourcelist/ResourceListClass.java.template",
                    "com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties"
            ));
        }

        @Test
        void testNoIncludesOrExcludes() throws MojoExecutionException {
            Log log = mock(Log.class);
            ResourcesScanner scanner = new ResourcesScanner(null, null, false, log);
            Path baseDir = Paths.get("src/main/resources");

            Set<String> resources = scanner.scan(baseDir);

            assertThat(resources, contains(
                    "META-INF/m2e/lifecycle-mapping-metadata.xml",
                    "com/github/robtimus/maven/plugins/resourcelist/ResourceListClass.java.template",
                    "com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties"
            ));
        }

        @Test
        void testCustomIncludes() throws MojoExecutionException {
            String[] includes = {"**/*.xml"};
            Log log = mock(Log.class);
            ResourcesScanner scanner = new ResourcesScanner(includes, null, false, log);
            Path baseDir = Paths.get("src/main/resources");

            Set<String> resources = scanner.scan(baseDir);

            assertThat(resources, contains(
                    "META-INF/m2e/lifecycle-mapping-metadata.xml"
            ));
        }

        @Test
        void testCustomExcludes() throws MojoExecutionException {
            String[] excludes = {"**/*.properties"};
            Log log = mock(Log.class);
            ResourcesScanner scanner = new ResourcesScanner(null, excludes, false, log);
            Path baseDir = Paths.get("src/main/resources");

            Set<String> resources = scanner.scan(baseDir);

            assertThat(resources, contains(
                    "META-INF/m2e/lifecycle-mapping-metadata.xml",
                    "com/github/robtimus/maven/plugins/resourcelist/ResourceListClass.java.template"
            ));
        }

        @Test
        void testCustomAndDefaultExcludes() throws MojoExecutionException {
            String[] excludes = {"**/*.properties"};
            Log log = mock(Log.class);
            ResourcesScanner scanner = new ResourcesScanner(null, excludes, true, log);
            Path baseDir = Paths.get("src/main/resources");

            Set<String> resources = scanner.scan(baseDir);

            assertThat(resources, contains(
                    "com/github/robtimus/maven/plugins/resourcelist/ResourceListClass.java.template"
            ));
        }

        @Test
        void testExcludedFiles() throws MojoExecutionException {
            Log log = mock(Log.class);
            ResourcesScanner scanner = new ResourcesScanner(null, null, true, log);
            Path baseDir = Paths.get("src/main/resources");

            scanner.excludeFile(baseDir.resolve("com/github/robtimus/maven/plugins/resourcelist/ResourceListClass.java.template"));

            Set<String> resources = scanner.scan(baseDir);

            assertThat(resources, contains(
                    "com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties"
            ));
        }

        @Test
        void testClassFilesAreExcluded() throws MojoExecutionException {
            Log log = mock(Log.class);
            ResourcesScanner scanner = new ResourcesScanner(null, null, true, log);
            Path baseDir = Paths.get("target/classes");

            Set<String> resources = scanner.scan(baseDir);

            assertThat(resources, contains(
                    "com/github/robtimus/maven/plugins/resourcelist/ResourceListClass.java.template",
                    "com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties"
            ));
        }

        @Test
        void testIOException() {
            Log log = mock(Log.class);
            ResourcesScanner scanner = new ResourcesScanner(null, null, true, log);
            Path baseDir = Paths.get(UUID.randomUUID().toString());

            MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> scanner.scan(baseDir));

            NoSuchFileException cause = assertInstanceOf(NoSuchFileException.class, exception.getCause());
            assertEquals(cause.getMessage(), exception.getMessage());
        }
    }

    // The following two tests don't actually test the code but more the developer's understanding of SelectorUtils.matchPath

    @ParameterizedTest
    @CsvSource({
            "com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties, false",
            "META-INF/MANIFEST.MF,                                                                 true",
            "META-INF/LICENSE.txt,                                                                 true",
            "META-INF/LICENSE.md,                                                                  true",
            "META-INF/services/java.nio.file.spi.FileSystemProvider,                               true",
            "META-INF/m2e/lifecycle-mapping-metadata.xml,                                          true",
            "META-INF/maven/com.github.robtimus/resource-list-maven-plugin/plugin-help.xml,        true"
    })
    void testMatchesPatterns(String relativePath, boolean expected) {
        List<String> patterns = ResourcesScanner.DEFAULT_EXCLUDES;
        assertEquals(expected, ResourcesScanner.matches(relativePath, patterns));
    }

    @ParameterizedTest
    @CsvSource({
            "com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties, **/*.class,           false",
            "META-INF/MANIFEST.MF,                                                                 META-INF/MANIFEST.MF, true",
            "META-INF/LICENSE.txt,                                                                 META-INF/LICENSE.*,   true",
            "META-INF/LICENSE.md,                                                                  META-INF/LICENSE.*,   true",
            "META-INF/services/java.nio.file.spi.FileSystemProvider,                               META-INF/services/**, true",
            "META-INF/m2e/lifecycle-mapping-metadata.xml,                                          META-INF/m2e/**,      true",
            "META-INF/maven/com.github.robtimus/resource-list-maven-plugin/plugin-help.xml,        META-INF/maven/**,    true"
    })
    void testMatchesPattern(String relativePath, String pattern, boolean expected) {
        assertEquals(expected, ResourcesScanner.matches(relativePath, pattern));
    }
}
