package com.attibexx.old_learning_app

//Kotlinx Serialization importok
// SerialName as KotlinxSerialName felúlírjuk átnevezük
//az eredeti annotációt.
import kotlinx.serialization.SerialName as KotlinxSerialName
import kotlinx.serialization.Serializable

// Gson import (ezt hozzá kell adni!)
import com.google.gson.annotations.SerializedName as GsonSerializedName

@Serializable//Ez a Kotlinx Serializationnek szól
data class QuestionAnswer(
    //Kérdés || Question
    @KotlinxSerialName("question")//Kotlinx Serializationhoz || for Kotlix Serialization
    @GsonSerializedName("question")//Gsonhöz kell || for Gson
    val question: String,

    //Helyes válasz/ válaszok || Right answer/ answers
    @KotlinxSerialName("rightAnswers")//Kotlinx Serializationhoz || for Kotlix Serialization
    @GsonSerializedName("rightAnswers")//Gsonhöz kell || for Gson
    val rightAnswers: List<String>,
    //Válaszolva || Answered
    @KotlinxSerialName("answered")//Kotlinx Serializationhoz || for Kotlix Serialization
    @GsonSerializedName("answered")//Gsonhöz kell || for Gson
    var answered: Boolean
)