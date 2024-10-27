/*
 * TestResourceList.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Generated;

@SuppressWarnings({ "nls", "resource" })
@Generated("resource-list-maven-plugin")
final class TestResourceList {

    private static final String RESOURCE_LIST_RESOURCE = "/resource-list-maven-plugin.test-resources";

    private static final TestResourceList ABSOLUTE = new TestResourceList(s -> "/" + s); //$NON-NLS-1$
    private static final TestResourceList RELATIVE = new TestResourceList(UnaryOperator.identity());

    private final UnaryOperator<String> resourceModifier;

    private TestResourceList(UnaryOperator<String> resourceModifier) {
        this.resourceModifier = resourceModifier;
    }

    static TestResourceList absolute() {
        return ABSOLUTE;
    }

    static TestResourceList relative() {
        return RELATIVE;
    }

    Stream<String> stream() {
        BufferedReader reader = reader();
        return reader.lines()
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .map(resourceModifier);
    }

    List<String> list() {
        try (BufferedReader reader = reader()) {
            return reader.lines()
                    .map(resourceModifier)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void forEach(Consumer<? super String> action) {
        Objects.requireNonNull(action);
        try (BufferedReader reader = reader()) {
            reader.lines()
                    .map(resourceModifier)
                    .forEach(action);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BufferedReader reader() {
        InputStream inputStream = getClass().getResourceAsStream(RESOURCE_LIST_RESOURCE);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        return new BufferedReader(inputStreamReader);
    }
}
