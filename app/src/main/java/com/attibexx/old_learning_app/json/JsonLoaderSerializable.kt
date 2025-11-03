package com.attibexx.old_learning_app.json

import android.content.Context
import android.net.Uri
import android.util.Log
import com.attibexx.old_learning_app.QuestionAnswer
import com.attibexx.old_learning_app.R
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class JsonLoaderSerializable(private val context: Context) : JsonProcessorFactory.JsonQuestionLoader {
    override fun readJsonQuestion(uri: Uri): List<QuestionAnswer> {
        val jsonParser = Json {
            //alapvető értékek || default values
            //val question: String,
            //    val answered: Boolean,
            //    val rightAnswer: String
            //más érték figyelmen kívül hagyása (ignoreUnknownKeys = true)
            //more values to be ignored (ignoreUnknownKeys = true)
            //pl Author ha lenne ilyen és figyelmen kivül hagyja
            //for example Author would be ignored if there is
            ignoreUnknownKeys = true
            //Ha nem tőkéletes a Json szabvány pl  hiányzik egy idézőjel ""
            //de még értelmezhető akkor beolvassa nem dob hibát.
            //If the Json standard is not perfect, e.g. a quote "" is missing
            //but it is still interpretable then it reads it and does not throw an error.
            isLenient = true
        }
        try {
            // Az InputStream megnyitása a ContentResolver segítségével
            // The InputStream is opened with the ContentResolver
            val inputStream =
                context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                inputStream.use { stream ->
                    val jsonString = stream.bufferedReader().use { it.readText() }
                    //megmondjuk a jsonfordítónak allakítsa
                    return jsonParser.decodeFromString<List<QuestionAnswer>>(jsonString)
                }
            } else {
                // Ha az inputStream null, nem sikerült megnyitni a fájlt
                Log.e("MainActivity", context.getString(
                    R.string.inputStreamNullText))
                return emptyList()
            }
        } catch (e: Exception) {
            // Hiba az InputStream olvasása közben
            Log.e(
                "MainActivity",
                "IOException: " + context.getString(
                    R.string.jsonParseError1) + ": ${e.localizedMessage}",
                e
            )

        } catch (e: SerializationException) {
            // Hiba a JSON deszerializálása közben
            Log.e(
                "MainActivity",
                "SerializationException: " + context.getString(
                    R.string.jsonParseError2) + ": ${e.localizedMessage}",
                e
            )
        }
       return emptyList()
    }
}