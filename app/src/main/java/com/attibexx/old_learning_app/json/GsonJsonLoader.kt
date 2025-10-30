package com.attibexx.old_learning_app.json

import android.content.Context
import android.net.Uri
import android.util.Log
import com.attibexx.old_learning_app.QuestionAnswer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken // Fontos a listákhoz!

class GsonJsonLoader(private val context: Context) {
    fun readJsonQuestion(uri: Uri): List<QuestionAnswer> {
        val jsonGsonParser = Gson()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = inputStream.bufferedReader()
                //A Gsonnak meg kell adni a lista pontos típusát
                //Gson needs to know the exact type of the list
                val questionListType = object : TypeToken<List<QuestionAnswer>>() {}.type
                //A dekódoláshoz jsonGsonParser.fromJson függvény kell
                //jsonGsonParser.fromJson function is needed for decoding
                return jsonGsonParser.fromJson(reader, questionListType)
            } else {
                Log.e("MainActivity", "InputStream is null")
            }
        } catch (e: Exception) {
            Log.e(
                "MainActivity",
                "(IOException) Error reading or parsing JSON with Gson: ${e.message}",
                e
            )
        }
        return emptyList()
    }
}



