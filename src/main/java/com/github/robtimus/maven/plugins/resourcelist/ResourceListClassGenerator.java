/*
 * ResourceListClassGenerator.java
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

final class ResourceListClassGenerator {

    @SuppressWarnings("nls")
    void generateClassFile(ResourceListClass resourceListClass, Resource resourceListResource) throws MojoExecutionException {
        int index = resourceListClass.className.lastIndexOf('.');
        String packageName = resourceListClass.className.substring(0, index);
        String className = resourceListClass.className.substring(index + 1);
        String resourceListFile = resourceListFile(resourceListResource);
        String visibility = resourceListClass.publicVisibility ? "public " : "";

        try {
            String template = readTemplate();
            String content = template
                    .replace("${package}", packageName)
                    .replace("${className}", className)
                    .replace("${resourceListFile}", resourceListFile)
                    .replace("${visibility}", visibility);

            Path packageDir = resourceListClass.outputDirectory.resolve(packageName.replace('.', '/'));
            Files.createDirectories(packageDir);

            Path classFile = packageDir.resolve(className + ".java");
            try (Writer writer = Files.newBufferedWriter(classFile)) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String readTemplate() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("ResourceListClass.java.template")) { //$NON-NLS-1$
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            inputStream.transferTo(outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }

    private String resourceListFile(Resource resource) {
        String fileName = resource.getIncludes().get(0);
        String dirName = resource.getTargetPath();
        return dirName.isEmpty() ? fileName : String.format("%s/%s", dirName, fileName); //$NON-NLS-1$
    }
}
