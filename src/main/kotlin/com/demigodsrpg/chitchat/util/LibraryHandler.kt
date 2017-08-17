/*
 * Copyright 2015 Demigods RPG
 * Copyright 2015 Alexander Chauncey
 * Copyright 2015 Alex Bennett
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.demigodsrpg.chitchat.util

import org.bukkit.plugin.Plugin
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.util.*

class LibraryHandler
(private val PLUGIN: Plugin) {

    private val FILE_NAMES: MutableList<String>
    private val LIB_DIRECTORY: File

    init {
        FILE_NAMES = ArrayList()
        LIB_DIRECTORY = File(PLUGIN.dataFolder.path + "/lib")
        checkDirectory()
    }

    // -- HELPER METHODS -- //

    fun addMavenLibrary(repo: String, groupId: String, artifactId: String, version: String) {
        try {
            val fileName = "$artifactId-$version.jar"
            loadLibrary(fileName, URI(repo + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" +
                    fileName).toURL())
        } catch (oops: Exception) {
            oops.printStackTrace()
        }

    }

    fun checkDirectory() {
        // If it exists and isn't a directory, throw an error
        if (LIB_DIRECTORY.exists() && !LIB_DIRECTORY.isDirectory) {
            PLUGIN.logger.severe("The library directory isn't a directory!")
            return
        } else if (!LIB_DIRECTORY.exists()) {
            LIB_DIRECTORY.mkdirs()
        }// Otherwise, make the directory

        // Check if all libraries exist

        val filesArray = LIB_DIRECTORY.listFiles()
        val files = Arrays.asList(*filesArray ?: arrayOf())

        for (file in files) {
            if (file.name.endsWith(".jar")) {
                FILE_NAMES.add(file.name)
            }
        }
    }

    fun loadLibrary(fileName: String, url: URL) {
        // Check if the files are found or not
        var libraryFile: File? = null
        if (FILE_NAMES.contains(fileName)) {
            libraryFile = File(LIB_DIRECTORY.toString() + "/" + fileName)
        }

        // If they aren't found, download them
        if (libraryFile == null) {
            PLUGIN.logger.warning("Downloading $fileName.")
            libraryFile = downloadLibrary(fileName, url)
        }

        // Add the library to the classpath
        addToClasspath(libraryFile)
    }

    fun addToClasspath(file: File?) {
        try {
            ClassPathHack.addFile(file!!, PLUGIN.javaClass.classLoader as URLClassLoader)
        } catch (oops: Exception) {
            PLUGIN.logger.severe("Couldn't load " + (if (file != null) file.name else "a required library") + ", " +
                    "this may cause problems.")
            oops.printStackTrace()
        }

    }

    fun downloadLibrary(libraryFileName: String, libraryUrl: URL): File? {
        // Get the file
        val libraryFile = File(LIB_DIRECTORY.path + "/" + libraryFileName)

        // Create the streams
        var `in`: BufferedInputStream? = null
        var fout: FileOutputStream? = null

        try {
            // Setup the streams
            `in` = BufferedInputStream(libraryUrl.openStream())
            fout = FileOutputStream(libraryFile)

            // Create variables for loop
            val data = ByteArray(BYTE_SIZE)
            var count: Int

            // Write the data to the file
            do {
                count = `in`.read(data, 0, BYTE_SIZE)
                if (count != -1) {
                    fout.write(data, 0, count)
                }
            } while (count != -1)

            PLUGIN.logger.info("Download complete.")

            // Return the file
            return libraryFile
        } catch (oops: Exception) {
            // Couldn't download the file
            PLUGIN.logger.severe("Download could not complete")
        } finally {
            // Close the streams
            try {
                if (`in` != null) {
                    `in`.close()
                }
                if (fout != null) {
                    fout.close()
                }
            } catch (ignored: Exception) {
            }
        }

        return null
    }

    companion object {
        // -- IMPORTANT FIELDS -- //

        val MAVEN_CENTRAL = "http://central.maven.org/maven2/"
        private val BYTE_SIZE = 1024
    }
}
