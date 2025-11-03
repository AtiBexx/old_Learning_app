package com.attibexx.old_learning_app.json

import android.content.Context
import android.net.Uri
import android.util.Log
import com.attibexx.old_learning_app.QuestionAnswer
import com.attibexx.old_learning_app.R
import org.json.JSONArray
import org.json.JSONException

class JsonLoader(private val context: Context) : JsonProcessorFactory.JsonQuestionLoader {
    override fun readJsonQuestion(uri: Uri): List<QuestionAnswer> {
        // Erre a jsonParser objektumra már nincs szükség
        // val jsonParser = Json { ... }

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // A String beolvasása
                //The String is read
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                // Létrehozunk egy üres, MÓDOSÍTHATÓ listát, amibe gyűjteni fogunk
                // Create an empty, MODIFIABLE list to collect into
                val resultList = mutableListOf<QuestionAnswer>()

                // A teljes szöveget egy JSONArray objektummá alakítjuk
                // Convert the entire text into a JSONArray object
                val jsonArray = JSONArray(jsonString)

                // Végigmegyünk a tömb összes elemén egy ciklussal
                // We go through all the elements of the array in a loop
                for (i in 0 until jsonArray.length()) {
                    // 4. Kivesszük az aktuális elemet, ami egy JSONObject
                    val jsonObject = jsonArray.getJSONObject(i)

                    // Kézzel kiolvassuk a kulcsokhoz tartozó értékeket
                    // Manually read the values for the keys
                    val question = jsonObject.getString("question")
                    val answered = jsonObject.getBoolean("answered")
                    val rightAnswersJsonArray = jsonObject.getJSONArray("rightAnswers")

                    // A JSON tömbből létrehozunk egy Kotlin List<String>-et.
                    // We create a Kotlin List<String> from the JSON array.
                    val rightAnswersList = mutableListOf<String>()
                    for (j in 0 until rightAnswersJsonArray.length()) {
                        rightAnswersList.add(rightAnswersJsonArray.getString(j))
                    }

                    // Az értékekből kézzel létrehozzuk az adat-objektumot
                    // We manually create the data object from the values
                    val questionAnswer = QuestionAnswer(
                        question = question,
                        answered = answered,
                        rightAnswers = rightAnswersList
                    )

                    // Hozzáadjuk az új objektumot a listánkhoz
                    // We add the new object to our list
                    resultList.add(questionAnswer)
                }

                // Visszaadjuk a feltöltött listát
                // We return the uploaded list
                return resultList

            } else {
                Log.e("MainActivity", context.getString(R.string.inputStreamNullText))
                return emptyList()
            }
        } catch (e: JSONException) {
            // Speciális hiba, ha a JSON formátuma rossz
            // Special error if the JSON format is wrong
            Log.e(
                "MainActivity",
                "JSONException: " + context.getString(
                    R.string.jsonParseError2) + ": ${e.localizedMessage}",
                e
            )
            return emptyList()
        } catch (e: Exception) {
            // Egyéb hibák (pl. fájl olvasása)
            Log.e(
                "MainActivity",
                "IOException: " + context.getString(
                    R.string.jsonParseError1) + ": ${e.localizedMessage}",
                e
            )
            return emptyList()
        }
    }
}
