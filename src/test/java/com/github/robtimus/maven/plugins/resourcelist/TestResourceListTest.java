/*
 * TestResourceListTest.java
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class TestResourceListTest {

    @Nested
    class Absolute {

        @Test
        void testStream() {
            try (Stream<String> resources = TestResourceList.absolute().stream()) {
                List<String> result = resources.collect(Collectors.toList());
                assertEquals(List.of("/com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties"), result);
            }
        }

        @Test
        void testList() {
            List<String> resources = TestResourceList.absolute().list();
            assertEquals(List.of("/com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties"), resources);
        }

        @Test
        void testForEach() {
            @SuppressWarnings("unchecked")
            Consumer<String> action = mock(Consumer.class);

            TestResourceList.absolute().forEach(action);

            verify(action).accept("/com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties");
            verifyNoMoreInteractions(action);
        }
    }

    @Nested
    class Relative {

        @Test
        void testStream() {
            try (Stream<String> resources = TestResourceList.relative().stream()) {
                List<String> result = resources.collect(Collectors.toList());
                assertEquals(List.of("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties"), result);
            }
        }

        @Test
        void testList() {
            List<String> resources = TestResourceList.relative().list();
            assertEquals(List.of("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties"), resources);
        }

        @Test
        void testForEach() {
            @SuppressWarnings("unchecked")
            Consumer<String> action = mock(Consumer.class);

            TestResourceList.relative().forEach(action);

            verify(action).accept("com/github/robtimus/maven/plugins/resourcelist/resource-list-maven-plugin.properties");
            verifyNoMoreInteractions(action);
        }
    }
}
