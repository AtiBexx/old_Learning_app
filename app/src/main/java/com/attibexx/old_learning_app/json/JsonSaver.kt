package com.attibexx.old_learning_app.json

//import android.content.Context
import android.util.Log
import com.attibexx.old_learning_app.QuestionAnswer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

//class JsonSaver(private val context: Context) {

object JsonSaver {

    //jsonformatter létrehozása
    //Json formatter creation
    private val jsonFormatter = Json { prettyPrint = true }

    //gsonFormatter létrehozása
    //gson formatter creation
    private val gsonFormatter: Gson = GsonBuilder().setPrettyPrinting().create()

    //companion object {
        // Logoláshoz használt címke
        // Label used for logging
        private const val TAG = "CreateQuestionActivity"

        //A c++ natív könyvtár betöltése
        //Loading the c++ native library
        // A 'native-lib' névnek meg kell egyeznie a build.gradle-ben
        // és a CMakeLists.txt-ben definiált könyvtár nevével.
        // A 'native-lib' name must match the name defined in the CMakeLists.txt
        // and the build.gradle
        init {
            System.loadLibrary("native-lib")
        }
    //}

    //Json fájl létrehozása (serializációs verzió)
    //Json file creation (serialization version)
    fun createJsonString(questionAnswerPairs: List<QuestionAnswer>): String? {
        return try {
            jsonFormatter.encodeToString(
                ListSerializer(QuestionAnswer.serializer()),
                questionAnswerPairs
            )
        } catch (e: Exception) {
            //Most rendszer üzenetett írunk
            //Now We write a system message
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Error encoding JSON with serialization", e)
            }
            //A hibát egy null érték visszaadásával jelezük
            null
        }
    }

    //Json fájl léterehozása sima verzió(alap ami benne van)
    //Json file creation simple version(the one in the base)
    fun createSimpleJsonString(questionAnswerPairs: List<QuestionAnswer>): String? {
        return try {
            //Létrehozuk a fő JSON tömböt(Array list)
            val rootArray = JSONArray()
            //Végigmegyünk minden kérdés-válasz páron a listában
            for (pair in questionAnswerPairs) {

                //létrehozuk a kérdés objektumot
                val questionObject = JSONObject()

                //beleteszük az objektumba a kulcs-érték párokat.
                //Kérdés-hez || for question
                questionObject.put("question", pair.question)
                //válaszolva boolean-hoz || for answered boolean
                questionObject.put("answered", pair.answered)

                //A válaszok listájához is létrehozunk egy tömböt(Array)
                //We also create an array for the list of answers.
                //és hozzáadjuk a "válaszolva" booleant is.
                //We add the "answered" boolean to the array.
                val answerArray = JSONArray(pair.rightAnswers)
                questionObject.put("rightAnswers", answerArray)

                //A Kész Json objektumot hozzáadjuk a fő tömbhöz.
                rootArray.put(questionObject)
            }
            //A teljes, felépített JSON tömböt szépen formázott stringgé
            // alakítjuk (4 szóközös behúzással).
            // Convert the entire, structured JSON array to a nicely
            // formatted string with (indented 4 spaces).
            rootArray.toString(4)
        } catch (e: Exception) {
            if (Log.isLoggable(TAG,Log.ERROR)) {
                Log.e(TAG, "Error encoding JSON manually", e)
            }
            return null // Hiba esetén null-t adunk vissza.|| Return null in case of an error.
        }
    }

    //Json fájl létrehozás(Gson verzió)
    //Json file creation(Gson version)
    fun createGsonString(questionAnswerPairs: List<QuestionAnswer>): String? {
        return try {
            // A gsonFormatter.toJson() metódusát használjuk a lista konvertálására.
            // We use the gsonFormatter.toJson() method to convert the list.
            gsonFormatter.toJson(questionAnswerPairs)
        } catch (e: Exception) {
            //Rendszerüzenet logolása hiba esetén
            //System message logging in case of an error
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Error encoding JSON with Gson", e)
            }

            //A hibát null értékel jelezzük az Activity felé
            //We notify the user in the Activity
            null
        }
    }

    //Json fájl letrehozása c++-al amit jni-vel hívunk meg
    //Creating a Json file with C++ that is called with JNI
    external fun createJsonStringWithCpp(questionAnswerPairs: List<QuestionAnswer>): String?

}


