package com.attibexx.old_learning_app.json

import android.content.Context
import android.net.Uri
import com.attibexx.old_learning_app.AppSettingsKeys.JSON_LOADER_MODE
import com.attibexx.old_learning_app.AppSettingsKeys.PREFS_FILE_NAME
import com.attibexx.old_learning_app.QuestionAnswer

/**
 * Ez az objektum felelős azért, hogy a felhasználó által a Beállításokban
 * kiválasztott JSON feldolgozó motort létrehozza és visszaadja.
 * Ez egy "gyár" (factory), ami a megfelelő betöltőt és mentőt "gyártja le".
 */

object JsonProcessorFactory {



    // Ezek az értékek, amiket a kulcs alatt tárolunk
    const val MODE_SERIALIZABLE = "serializable"
    const val MODE_MANUAL_JSON = "manual_json"
    const val MODE_GSON = "gson"
    const val MODE_CPP = "cpp"
    // Loader szekció
    // A közös interface a betöltéshez
    // Loader section
    // The common interface for loading
    interface JsonQuestionLoader {
        fun readJsonQuestion(uri: Uri): List<QuestionAnswer>
    }

    /**
     * Ez a függvény a szám alapján visszaadja a megfelelő betöltöt.
     * This function returns the corresponding loader based on the number.
     *
     */
    fun create(context: Context): JsonQuestionLoader {
        val loaderMode: String? = getJsonLoaderMode(context)

        // A when már a STRING-ekkel dolgozik
        return when (loaderMode) {
            MODE_SERIALIZABLE -> JsonLoaderSerializable(context)
            MODE_GSON -> GsonJsonLoader(context)
            MODE_MANUAL_JSON -> JsonLoader(context)
            MODE_CPP -> CppJsonLoader(context)
            // Alapértelmezett eset, ha valamiért ismeretlen a mód
            else -> JsonLoaderSerializable(context)
        }
    }
    // Mentés szekció
    // Saver Section
    interface QuestionSaver {
        fun createJsonString(questionAnswerPairs: List<QuestionAnswer>): String?
    }

    /**
     * Ez a függvény a szám alapján visszaadja a megfelelő mentöt(elmentet verziót).
     * This function returns the corresponding save (saved version) based on the number.
     */
    fun createSaver(context: Context): QuestionSaver {
        val loaderMode = getJsonLoaderMode(context)

        return when (loaderMode) {

            // Ha a betöltő a GSON, a mentő is az legyen.
            MODE_GSON -> object : QuestionSaver {
                override fun createJsonString(questionAnswerPairs: List<QuestionAnswer>): String? {
                    return JsonSaver.createGsonString(questionAnswerPairs)
                }
            }

            // Ha a betöltő a C++, a mentő is az legyen.
            MODE_CPP -> object : QuestionSaver {
                override fun createJsonString(questionAnswerPairs: List<QuestionAnswer>): String? {
                    return JsonSaver.createJsonStringWithCpp(questionAnswerPairs)
                }
            }

            // Ha a betöltő a manuális/beépített, a mentő is az legyen.
            MODE_MANUAL_JSON -> object : QuestionSaver {
                override fun createJsonString(questionAnswerPairs: List<QuestionAnswer>): String? {
                    return JsonSaver.createSimpleJsonString(questionAnswerPairs)
                }
            }

            else -> object : QuestionSaver {
                override fun createJsonString(questionAnswerPairs: List<QuestionAnswer>): String? {
                    return JsonSaver.createJsonString(questionAnswerPairs)
                }
            }
        }
    }


    /**
     * Ez agy privát segédfüggvény, ami lekéri a mentett választást,
     * hogy ne kelljen mindkét create függvényben megismételni a kódot.
     * This is a private helper function that retrieves the saved selection,
     * so that you don't have to repeat the code in both create functions.
     */
    private fun getJsonLoaderMode(context: Context): String? {
        // Használjuk a központi beállítás-fájl nevét
        // Use the name of the central configuration file
        val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

        // Olvassuk ki a STRING értéket a megfelelő kulcs alól
        // Read the STRING value from the corresponding key
        return prefs.getString(JSON_LOADER_MODE, MODE_SERIALIZABLE)
    }
}