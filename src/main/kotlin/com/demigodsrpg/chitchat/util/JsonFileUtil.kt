package com.demigodsrpg.chitchat.util

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.*

class JsonFileUtil(private val FOLDER: File, private val PRETTY: Boolean) {

    private fun createFile(file: File) {
        try {
            file.parentFile.mkdirs()
            file.createNewFile()
        } catch (oops: Exception) {
            oops.printStackTrace()
        }
    }

    fun removeFile(key: String) {
        val file = File(FOLDER.path + "/" + key + ".json")
        if (file.exists()) {
            file.delete()
        }
    }

    fun saveToFile(key: String, data: Map<*, *>?) {
        if (data != null) {
            val file = File(FOLDER.path + "/" + key + ".json")
            if (!file.exists()) {
                createFile(file)
            }
            val gson = if (PRETTY) GsonBuilder().setPrettyPrinting().create() else GsonBuilder().create()
            val json = gson.toJson(data)
            try {
                val writer = PrintWriter(file)
                writer.print(json)
                writer.close()
            } catch (oops: Exception) {
                oops.printStackTrace()
            }

        }
    }

    fun loadFromFile(key: String): Map<*, *> {
        val gson = GsonBuilder().create()
        try {
            val file = File(FOLDER.path + "/" + key + ".json")
            if (file.exists()) {
                val inputStream = FileInputStream(file)
                val reader = InputStreamReader(inputStream)
                val value = gson.fromJson(reader, Map::class.java)
                reader.close()
                return value
            }
        } catch (oops: Exception) {
            oops.printStackTrace()
        }

        return HashMap<Any, Any>()
    }
}
