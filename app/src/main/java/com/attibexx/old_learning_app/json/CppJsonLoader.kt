package com.attibexx.old_learning_app.json

import android.content.Context
import android.net.Uri
import android.util.Log
import com.attibexx.old_learning_app.QuestionAnswer
import com.attibexx.old_learning_app.R
import java.io.IOException

class CppJsonLoader(private val context: Context) : JsonProcessorFactory.JsonQuestionLoader {

    override fun readJsonQuestion(uri: Uri): List<QuestionAnswer> {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // A C++ kódnak a teljes JSON stringre van szüksége.
                // A C++ code needs the entire JSON string.
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                // Meghívjuk a natív C++ függvényt a stringgel.
                // A C++ kód adja vissza a kész List<QuestionAnswer> objektumot.
                // We call the native C++ function with the string.
                // The C++ code returns the completed List<QuestionAnswer> object.
                return readJsonStringWithCpp(jsonString)
            } else {
                Log.e("CppJsonLoader", context.getString(R.string.inputStreamNullText))
                return emptyList()
            }
        } catch (e: IOException) {
            Log.e("CppJsonLoader", "Error reading file: ${e.message}", e)
            return emptyList()
        } catch (e: Exception) {
            // Ez a C++ kódból dobott esetleges RuntimeException-t is elkapja.
            Log.e("CppJsonLoader", "Error during C++ JSON parsing: ${e.message}", e)
            return emptyList()
        }
    }
    //Itt hívjuk meg a Natív c++ függvényünket
    //This is where we call our Native C++ function
    private external fun readJsonStringWithCpp(jsonString: String): List<QuestionAnswer>

    companion object{
        init {
            try {
                System.loadLibrary("native-lib")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("CppJsonLoader", "Error loading native library: ${e.message}", e)
            }
        }
    }

}
