/*
 * ResourceListClassGeneratorTest.java
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
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import com.github.robtimus.filesystems.memory.MemoryFileAttributeView;
import com.github.robtimus.filesystems.memory.MemoryFileSystemProvider;
import com.github.robtimus.junit.support.extension.testresource.TestResource;

@SuppressWarnings("nls")
class ResourceListClassGeneratorTest {

    @Nested
    class GenerateClassFile {

        @BeforeEach
        void clearMemoryFileSystem() {
            MemoryFileSystemProvider.clear();
        }

        @Test
        void testTestResourceList() throws MojoExecutionException, IOException {
            ResourceListClass resourceListClass = new ResourceListClass();
            resourceListClass.className = "com.github.robtimus.maven.plugins.resourcelist.TestResourceList";
            resourceListClass.publicVisibility = false;

            Resource resourceListResource = new Resource();
            resourceListResource.addInclude("resource-list-maven-plugin.test-resources");
            resourceListResource.setTargetPath("");

            resourceListClass.outputDirectory = Paths.get(URI.create("memory:/generated"));

            ResourceListClassGenerator generator = new ResourceListClassGenerator();
            generator.generateClassFile(resourceListClass, resourceListResource);

            Path classFile = resourceListClass.outputDirectory.resolve("com/github/robtimus/maven/plugins/resourcelist/TestResourceList.java");

            String content = MemoryFileSystemProvider.getContentAsString(classFile);

            String expectedContent = Files.readString(Paths.get("src/test/java/com/github/robtimus/maven/plugins/resourcelist/TestResourceList.java"))
                    .replaceAll("\\Q@SuppressWarnings({ \"nls\", \"resource\" })\\E\r?\n", "")
                    .replaceAll("(?s).*\npackage", "package");

            assertEquals(expectedContent, content);
        }

        @Test
        void testPublicWithRootResource(@TestResource("ResourceList.java.public.rootResource") String expectedContent)
                throws MojoExecutionException, IOException {

            test(true, "", expectedContent);
        }

        @Test
        void testPublicWithNonRootResource(@TestResource("ResourceList.java.public.nonRootResource") String expectedContent)
                throws MojoExecutionException, IOException {

            test(true, "subdir", expectedContent);
        }

        @Test
        void testNonPublicWithRootResource(@TestResource("ResourceList.java.nonPublic.rootResource") String expectedContent)
                throws MojoExecutionException, IOException {

            test(false, "", expectedContent);
        }

        @Test
        void testNonPublicWithNonRootResource(@TestResource("ResourceList.java.nonPublic.nonRootResource") String expectedContent)
                throws MojoExecutionException, IOException {

            test(false, "subdir", expectedContent);
        }

        private void test(boolean publicVisibility, String targetPath, String expectedContent)
                throws MojoExecutionException, IOException {

            ResourceListClass resourceListClass = new ResourceListClass();
            resourceListClass.className = "com.github.robtimus.maven.plugins.resourcelist.ResourceList";
            resourceListClass.publicVisibility = publicVisibility;
            resourceListClass.outputDirectory = Paths.get(URI.create("memory:/generated"));

            Resource resourceListResource = new Resource();
            resourceListResource.addInclude("test.resources");
            resourceListResource.setTargetPath(targetPath);

            ResourceListClassGenerator generator = new ResourceListClassGenerator();
            generator.generateClassFile(resourceListClass, resourceListResource);

            Path classFile = resourceListClass.outputDirectory.resolve("com/github/robtimus/maven/plugins/resourcelist/ResourceList.java");

            String content = MemoryFileSystemProvider.getContentAsString(classFile);

            assertEquals(expectedContent, content);
        }

        @Test
        void testWriteError() throws IOException {
            ResourceListClass resourceListClass = new ResourceListClass();
            resourceListClass.className = "com.github.robtimus.maven.plugins.resourcelist.TestResourceList";
            resourceListClass.publicVisibility = false;

            Resource resourceListResource = new Resource();
            resourceListResource.addInclude("resource-list-maven-plugin.test-resources");
            resourceListResource.setTargetPath("");

            resourceListClass.outputDirectory = Paths.get(URI.create("memory:/generated"));

            Files.createDirectories(resourceListClass.outputDirectory);
            Files.getFileAttributeView(resourceListClass.outputDirectory, MemoryFileAttributeView.class).setReadOnly(true);

            ResourceListClassGenerator generator = new ResourceListClassGenerator();

            MojoExecutionException exception = assertThrows(MojoExecutionException.class,
                    () -> generator.generateClassFile(resourceListClass, resourceListResource));

            AccessDeniedException cause = assertInstanceOf(AccessDeniedException.class, exception.getCause());
            assertEquals(resourceListClass.outputDirectory.toString(), cause.getFile());

            Path classFile = resourceListClass.outputDirectory.resolve("com/github/robtimus/maven/plugins/resourcelist/TestResourceList.java");

            assertFalse(Files.exists(classFile));
        }

        @Nested
        class GeneratedCode {

            @TempDir
            private Path baseDir;

            @CartesianTest
            void testCodeCompiles(
                    @CartesianTest.Values(booleans = { true, false }) boolean publicVisibility,
                    @CartesianTest.Values(strings = { "", "subdir" }) String targetPath)
                            throws MojoExecutionException, IOException, ReflectiveOperationException {

                ResourceListClass resourceListClass = new ResourceListClass();
                resourceListClass.className = "test.ResourceList";
                resourceListClass.publicVisibility = publicVisibility;
                resourceListClass.outputDirectory = baseDir;

                Resource resourceListResource = new Resource();
                resourceListResource.addInclude("test.resources");
                resourceListResource.setTargetPath(targetPath);

                ResourceListClassGenerator generator = new ResourceListClassGenerator();

                generator.generateClassFile(resourceListClass, resourceListResource);

                String expectedResource = ("/" + targetPath + "/test.resources").replace("//", "/");

                compileCode();

                validateGeneratedClass(publicVisibility, expectedResource);
            }

            private void compileCode() throws IOException {
                File sourceFile = baseDir.resolve("test/ResourceList.java").toFile();

                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

                try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {

                    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sourceFile);

                    CompilationTask compilationTask = compiler.getTask(null, fileManager, null, null, null, compilationUnits);

                    Boolean compiledSuccessfully = compilationTask.call();
                    assertEquals(Boolean.TRUE, compiledSuccessfully);
                }
            }

            private void validateGeneratedClass(boolean publicVisibility, String expectedResource) throws IOException, ReflectiveOperationException {
                URL[] urls = { baseDir.toUri().toURL() };
                try (URLClassLoader classLoader = new URLClassLoader(urls)) {
                    Class<?> resourceListClass = Class.forName("test.ResourceList", true, classLoader);

                    MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(resourceListClass, MethodHandles.lookup());

                    assertVisibility(resourceListClass.getModifiers(), publicVisibility);

                    validateConstructors(resourceListClass);

                    validateFields(resourceListClass, expectedResource, lookup);

                    validateMethods(resourceListClass, publicVisibility, lookup);
                }
            }

            private void validateConstructors(Class<?> resourceListClass) {
                Constructor<?>[] constructors = resourceListClass.getDeclaredConstructors();
                assertThat(constructors, arrayWithSize(1));
                assertArrayEquals(new Class[] { UnaryOperator.class }, constructors[0].getParameterTypes());
                assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
            }

            private void validateFields(Class<?> resourceListClass, String expectedResource, MethodHandles.Lookup lookup) {
                Map<String, Field> fields = Arrays.stream(resourceListClass.getDeclaredFields())
                        .filter(field -> !field.isSynthetic())
                        .collect(Collectors.toMap(Field::getName, Function.identity()));

                assertEquals(Set.of("RESOURCE_LIST_RESOURCE", "ABSOLUTE", "RELATIVE", "resourceModifier"), fields.keySet());

                fields.values().forEach(field -> {
                    assertTrue(Modifier.isPrivate(field.getModifiers()));
                    assertNotEquals("resourceModifier".equals(field.getName()), Modifier.isStatic(field.getModifiers()));
                    assertTrue(Modifier.isFinal(field.getModifiers()));
                });

                String actualResource = (String) assertDoesNotThrow(() -> lookup
                        .findStaticVarHandle(resourceListClass, "RESOURCE_LIST_RESOURCE", String.class))
                        .get();

                assertEquals(expectedResource, actualResource);
            }

            private void validateMethods(Class<?> resourceListClass, boolean publicVisibility, MethodHandles.Lookup lookup) {
                Map<String, Method> methods = Arrays.stream(resourceListClass.getDeclaredMethods())
                        .filter(method -> !method.isSynthetic())
                        .collect(Collectors.toMap(Method::getName, Function.identity()));

                assertEquals(Set.of("absolute", "relative", "stream", "list", "forEach", "reader"), methods.keySet());

                validateFactoryMethod(methods.get("absolute"), "ABSOLUTE", publicVisibility, resourceListClass, lookup);
                validateFactoryMethod(methods.get("relative"), "RELATIVE", publicVisibility, resourceListClass, lookup);

                validateAccessorMethod(methods.get("stream"), Stream.class, List.of(), publicVisibility);
                validateAccessorMethod(methods.get("list"), List.class, List.of(), publicVisibility);
                validateAccessorMethod(methods.get("forEach"), void.class, List.of(Consumer.class), publicVisibility);
            }

            private void validateFactoryMethod(Method method, String matchingField, boolean publicVisibility, Class<?> resourceListClass,
                    MethodHandles.Lookup lookup) {

                assertTrue(Modifier.isStatic(method.getModifiers()));
                assertVisibility(method.getModifiers(), publicVisibility);

                Object fieldValue = assertDoesNotThrow(() -> lookup
                        .findStaticVarHandle(resourceListClass, matchingField, resourceListClass))
                        .get();

                Object methodReturnValue = assertDoesNotThrow(() -> lookup
                        .findStatic(resourceListClass, method.getName(), MethodType.methodType(resourceListClass))
                        .invoke());

                assertSame(fieldValue, methodReturnValue);
            }

            private void validateAccessorMethod(Method method, Class<?> returnType, List<Class<?>> parameterTypes, boolean publicVisibility) {

                assertFalse(Modifier.isStatic(method.getModifiers()));
                assertVisibility(method.getModifiers(), publicVisibility);

                assertEquals(returnType, method.getReturnType());
                assertEquals(parameterTypes, Arrays.asList(method.getParameterTypes()));
            }

            private void assertVisibility(int modifiers, boolean publicVisibility) {
                assertFalse(Modifier.isPrivate(modifiers));
                assertFalse(Modifier.isProtected(modifiers));
                assertEquals(publicVisibility, Modifier.isPublic(modifiers));
            }
        }
    }
}
