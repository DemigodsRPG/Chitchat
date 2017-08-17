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

import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader

object ClassPathHack {
    private val parameters = arrayOf<Class<*>>(URL::class.java)

    @Throws(IOException::class)
    fun addFile(f: File, cL: URLClassLoader) {
        addURL(f.toURI().toURL(), cL)
    }

    @Throws(IOException::class)
    fun addURL(u: URL, cL: URLClassLoader) {
        val urlClassLoader = URLClassLoader::class.java
        try {
            val method = urlClassLoader.getDeclaredMethod("addURL", *parameters)
            method.isAccessible = true
            method.invoke(cL, u)
        } catch (t: Throwable) {
            t.printStackTrace()
            throw IOException("Error, could not add URL to system classloader")
        }
    }
}
