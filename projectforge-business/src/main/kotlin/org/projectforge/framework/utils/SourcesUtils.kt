/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.utils

import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.projectforge.framework.utils.SourcesUtils.extractAllClassnames
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

fun main() {
    extractAllClassnames(false).sorted().forEach { println(it) }
}

object SourcesUtils {
    private var basePath: Path? = null

    fun extractAllClassnames(includingNestedClass: Boolean = false): Set<String> {
        val classNames = mutableSetOf<String>()
        getSrcMainDirs().forEach { srcDir ->
            log.info { "Processing src dir: $srcDir" }
            listFiles(srcDir, "kt", "java").forEach { file ->
                classNames.addAll(extractClassnames(file, includingNestedClass))
            }
        }
        return classNames
    }

    /**
     * Extracts the class names from the given file.
     * Nested classes are represented by a dot-separated string.
     * @param file The file to extract the class names from.
     * @return A list of class names.
     */
    fun extractClassnames(file: File, includingNestedClass: Boolean = false): List<String> {
        val classNames = mutableListOf<String>()
        val classStack = mutableListOf<String>()  // To track nested classes
        var braceLevel = 0  // To track block depth
        var packageName = ""  // To store the package name

        if (file.exists() && (file.extension == "kt" || file.extension == "java")) {
            var fileContent = file.readText()

            // 1. Remove single-line comments (// ...)
            fileContent = fileContent.replace(Regex("//.*"), "")

            // 2. Remove multi-line comments (/* ... */)
            fileContent = fileContent.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")

            // 3. Remove content within double quotes (string literals)
            fileContent = fileContent.replace(Regex("\"(\\\\\"|[^\"])*\""), "")

            // Process the cleaned content line by line
            fileContent.lines().forEach { line ->
                // Look for the package statement
                val packageRegex = Regex("""\bpackage\s+([\w\.]+)\b""")
                val packageMatch = packageRegex.find(line)

                if (packageMatch != null) {
                    packageName = packageMatch.groupValues[1]
                }

                // Refined regex for class, interface, enum declarations
                val classRegex = Regex("""\b(class|interface|object|enum)\s+([A-Z]\w*)\b(\s*\(.*\))?\s*\{?""")
                val classMatch = classRegex.find(line)

                // If a class is found
                if (classMatch != null) {
                    val className = classMatch.groupValues[2]

                    // If inside an inner block (braceLevel > 0)
                    if (classStack.isNotEmpty()) {
                        if (includingNestedClass) {
                            classNames.add("$packageName.${classStack.joinToString(".")}.$className")
                        }
                    } else {
                        classNames.add("$packageName.$className")
                    }

                    // Add class to stack for nested class tracking
                    classStack.add(className)
                }

                // Track block depth with { and }
                braceLevel += line.count { it == '{' }
                braceLevel -= line.count { it == '}' }

                // When the block ends, remove classes from stack
                if (braceLevel == 0 && classStack.isNotEmpty()) {
                    classStack.clear()
                }
            }
        }

        return classNames
    }

    fun parseClasses(file: File, includingNestedClass: Boolean = false): Set<String> {
        val classNames = mutableSetOf<String>()
        val classStack = mutableListOf<String>() // Stack for nested classes.
        var braceLevel = 0  // To track block depth
        val fileContent = file.readText()

        // 1. Remove single-line comments (// ...)
        val noSingleLineComments = fileContent.replace(Regex("//.*"), "")

        // 2. Remove multi-line comments (/* ... */)
        val cleanedContent = noSingleLineComments.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")

        // Process the cleaned content line by line
        cleanedContent.lines().forEach { line ->
            // Refined regex for class, interface, enum declarations
            val regex = Regex("""\b(class|interface|object|enum)\s+([A-Z]\w*)\b(\s*\(.*\))?\s*\{?""")
            val match = regex.find(line)

            // If a class is found
            if (match != null) {
                val className = match.groupValues[2]

                // If inside an inner block (braceLevel > 0)
                if (classStack.isNotEmpty()) {
                    classNames.add("${classStack.joinToString(".")}.$className")
                } else {
                    classNames.add(className)
                }

                // Add class to stack for nested class tracking
                classStack.add(className)
            }

            // Track block depth with { and }
            braceLevel += line.count { it == '{' }
            braceLevel -= line.count { it == '}' }

            // When the block ends, remove classes from stack
            if (braceLevel == 0 && classStack.isNotEmpty()) {
                classStack.clear()
            }
        }
        return classNames
    }

    /**
     * Returns the main directories of the project.
     */
    fun getSrcMainDirs(): Sequence<File> {
        return getDirs("main", "src")
    }

    /**
     * Returns the directories with the given names.
     * @param dirName The name of the directory.
     * @param parentDirName The name of the parent directory.*
     */
    fun getDirs(dirName: String, parentDirName: String): Sequence<File> {
        return getBasePath().toFile().walk().maxDepth(4)
            .filter { file -> file.isDirectory() && file.name == dirName && file.parentFile?.name == parentDirName }
    }

    /**
     * Returns the subdirectory with the given name.
     */
    fun getSubDir(dir: File, subDirName: String): File? {
        return dir.listFiles { _, name -> name == subDirName }?.firstOrNull()
    }

    /**
     * Returns a list of files with the given suffixes.
     * @param path The path to search for files.
     * @param suffix The suffixes of the files to search for.
     * @param recursive If true, the search is recursive.
     * @return A list of files with the given suffixes.
     */
    fun listFiles(path: File, vararg suffix: String, recursive: Boolean = true): Collection<File> {
        return FileUtils.listFiles(path, suffix, recursive)
    }

    /**
     * Returns the base path of the project (src-files).
     * It is determined by searching for the 'projectforge-application' directory.
     * If the directory is not found, the current directory is returned.
     * @return The base path of the project.
     */
    fun getBasePath(): Path {
        if (basePath != null) {
            return basePath!!
        }
        var path = Paths.get(System.getProperty("user.dir"))
        for (i in 0..10) { // Paranoia for avoiding endless loops
            val applicationDir = path.toFile().walk().maxDepth(1)
                .find { file -> file.name == "projectforge-application" }
            // val applicationDir = Files.walk(path, 1).toList().find { it.name == "projectforge-application" }
            if (applicationDir != null) {
                basePath = applicationDir.parentFile.toPath()
                log.info { "Using source directory '${basePath?.toAbsolutePath()}'." }
                return basePath!!
            }
            if (path.parent != null) {
                path = path.parent
            }
        }
        log.error { "Cannot find 'projectforge-application' directory in path '${path.toAbsolutePath()}'." }
        return path
    }
}
