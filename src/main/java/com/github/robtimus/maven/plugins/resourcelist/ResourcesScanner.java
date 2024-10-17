/*
 * ResourcesScanner.java
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.SelectorUtils;

final class ResourcesScanner {

    @SuppressWarnings("nls")
    static final List<String> DEFAULT_EXCLUDES = List.of(
            "META-INF/MANIFEST.MF",
            "META-INF/LICENSE.*",
            "META-INF/services/**",
            "META-INF/maven/**",
            "META-INF/m2e/**");

    private final List<String> includes;
    private final List<String> excludes;
    private final Set<Path> excludedFiles;
    private final Log log;

    ResourcesScanner(String[] includes, String[] excludes, boolean addDefaultExcludes, Log log) {
        this.includes = new ArrayList<>();
        if (includes != null) {
            Collections.addAll(this.includes, includes);
        }

        this.excludes = new ArrayList<>();
        if (excludes != null) {
            Collections.addAll(this.excludes, excludes);
        }
        if (addDefaultExcludes) {
            this.excludes.addAll(DEFAULT_EXCLUDES);
        }

        this.excludedFiles = new HashSet<>();
        this.log = log;
    }

    void excludeFile(Path file) {
        excludedFiles.add(file);
    }

    Set<String> scan(Path baseDir) throws MojoExecutionException {
        log.debug(Messages.ResourcesScanner.scanningResources(baseDir));
        log.debug(Messages.ResourcesScanner.includes(includes));
        log.debug(Messages.ResourcesScanner.excludes(excludes));

        try (Stream<Path> stream = Files.walk(baseDir)) {
            return stream
                    .filter(path -> !excludedFiles.contains(path))
                    .filter(Files::isRegularFile)
                    .map(file -> baseDir.relativize(file).toString().replace(File.separatorChar, '/'))
                    // don't consider class files to be resources
                    .filter(resource -> !resource.endsWith(".class")) //$NON-NLS-1$
                    .filter(resource -> isIncluded(resource, log))
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    boolean isIncluded(String relativePath, Log log) {
        boolean included = includes.isEmpty() || matches(relativePath, includes);
        if (!included) {
            log.debug(Messages.ResourcesScanner.resourceNotIncluded(relativePath));
            return false;
        }
        boolean excluded = matches(relativePath, excludes);
        if (excluded) {
            log.debug(Messages.ResourcesScanner.resourceExcluded(relativePath));
            return false;
        }
        log.debug(Messages.ResourcesScanner.resourceIncluded(relativePath));
        return true;
    }

    static boolean matches(String relativePath, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> matches(relativePath, pattern));
    }

    static boolean matches(String relativePath, String pattern) {
        return SelectorUtils.matchPath(pattern, relativePath, "/", true); //$NON-NLS-1$
    }
}
