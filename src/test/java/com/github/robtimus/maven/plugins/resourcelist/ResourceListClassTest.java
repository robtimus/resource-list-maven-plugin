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
import java.io.File;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class ResourceListClassTest {

    @Test
    void testSetOutputDirectory() {
        ResourceListClass resourceListClass = new ResourceListClass();
        resourceListClass.setOutputDirectory(new File("src/main/java/../../../target/classes"));

        assertEquals(Paths.get("target/classes").toAbsolutePath(), resourceListClass.outputDirectory);
    }
}
