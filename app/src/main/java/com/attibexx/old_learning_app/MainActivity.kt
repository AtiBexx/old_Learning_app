package com.attibexx.old_learning_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.attibexx.old_learning_app.databinding.ActivityMainBinding
import com.attibexx.old_learning_app.json.CppJsonLoader
import com.attibexx.old_learning_app.json.GsonJsonLoader
import com.attibexx.old_learning_app.json.JsonLoader
import com.attibexx.old_learning_app.json.JsonLoaderSerializable


class MainActivity : AppCompatActivity() {
    //Változok deklarálása
    //Variables declaration
    //Binding osztály példányosítása
    private lateinit var binding: ActivityMainBinding

    //1. A JsonBetöltö példánya (Kotlinx.Serialization alapú)
    //1. Json loader instance (Kotlinx.Serialization based)
    private val jsonLoaderSerializable by lazy { JsonLoaderSerializable(this) }

    // 2. A "sima" org.json alapú
    // 2. The "plain" org.json based
    private val jsonLoader by lazy { JsonLoader(this) }

    // 3. A Gson alapú
    // 3. The Gson based
    private val gsonJsonLoader by lazy { GsonJsonLoader(this) }

    // 4. A C++/JNI alapú
    // 4. The C++/JNI based
    private val cppJsonLoader by lazy { CppJsonLoader(this) }

    //aktuális kérdés indexe
    //index of current question
    private var currentQuestionIndex = 0

    //A kérdések tároló listája
    //List of questions
    private val questionList = mutableListOf<QuestionAnswer>()

    //Az összes kérdés száma
    //The number of all questions
    /*a get() biztosítja hogy ne manuálisan keljen a
    kérdések méretét frisíteni hanem a fájl és az adatok
    betöltésével változon*/
    private val totalQuestionsCount get() = questionList.size

    //A tárhely engedélyének a példányosítása
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<Intent>

    //Fájlválasztás deklarálása
    //File picker declaration
    private lateinit var filePickerLauncher: ActivityResultLauncher<String>

    //A prefs használatához példányosítás
    //Toast üzenetek állapotainak elmentésére ||
    private val prefs by lazy { getSharedPreferences("prefs", MODE_PRIVATE) }
    //a by lazy miatt nem kell elhelyzni az oncreate-ban
    //because of by lazy it doesn't need to be placed in oncreate
    /*private lateinit var prefs: SharedPreferences
    private const val PREFS_NAME = "MyLearningAppPrefs"*/

    /*
Companion Object:
Olyan objektum, ami az osztályhoz-hoz tartozik, nem egy pédányhoz.
Használjuk, ha kell egy "statikus"(nem fog változni)
változó vagy függvény Kotlinban.
An object that belongs to a class, not an instance.
Used when you need a "static" variable or function in Kotlin.

  class MyClass {
      companion object {
          val myStaticVal = 42
          fun myStaticFunc() = println("Hello")
      }
  }
- Hívás || Call:
  MyClass.myStaticVal
  MyClass.myStaticFunc()
*/
    companion object {
        //Json Saver MOde létrehozása
        //Json Saver Mode creation
        const val JSON_SAVER_MODE = "json_saver_mode"
        // Logoláshoz használt címke
        // Label used for logging
        private const val TAG = "MainActivity"

        //Az enegedély kérésének a kódja || The code of the permission request
        private const val REQUEST_PERMISSION_CONSTATE = 1

        //prefs-hez tartozó kulcs || prefs key
        private const val PREF_PERMISSION_TOAST_SHOWN = "permission_toast_shown"

        // JSON Feldolgozó Módok Kulcsai || JSON Loader Mode Keys
        // Ezt a kulcsot használjuk a SharedPreferences-ben a mód mentésére.
        // Use this key to store the mode in the SharedPreferences.
        const val JSON_LOADER_MODE = "json_loader_mode"

        // Ezek az értékek, amiket a kulcs alatt tárolunk
        const val MODE_SERIALIZABLE = "serializable"
        const val MODE_MANUAL_JSON = "manual_json"
        const val MODE_GSON = "gson"
        const val MODE_CPP = "cpp"
    }

    //onCreate függvény
    // onCreate function
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //a gombok eseménykezelőinek meghívása
        //call the button event handlers
        setupButtonListeners()
        //A tárhely engedélyének lekérdezése
        //Check the storage permission
        initializeLaunchers()
        updateUiBasedOnPermissions()

        /*prefs incializálása de nem kell a by lazy miatt
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)*/

        //binding main-re állítjuk a scroolok miatt
        //Set binding.root to'main'for the scroll
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
    }

    //ÁllapotVáltozások kezelése a képernyő elforgatásakor
    //Handling state changes when rotating the screen
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        /*A Hibakezelés Loggolására debug módban használjuk az if
        * szerkezettel a Log.isLoggable-t
        * For Error Handling Logging in debug mode, use Log.isLoggable
        * with the if construct */
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(
                TAG,
                "onConfigurationChanged: New orientation: ${newConfig.orientation}"
            )
        }
    }

    /*Tárhelyel kapcsolatos függvények
    Functions related to storage*/
//*****************************************
    //A Tárhely engedélyének a lekérdezése
    //Ask for storage permission
    private fun initializeLaunchers() {
        storagePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        //ha az Engedély már meg lett adva egyszer írja ki
                        //if the permission was already granted once write a message
                        showPermissionGrantedToastOnce()
                        //frísitjük az engedélyeket a UI-ra
                        //disable the permissions for ui
                        updateUiBasedOnPermissions()
                    } else {
                        //Írunk egy üzenetett a felhasználónak hogy engedélyre van szüksége
                        //write a message to the user that permission is needed
                        Toast.makeText(
                            this, getString(
                                R.string.checkStoragePermissionText2
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        //frísítjük az engedélykérést az UI-ra
                        //enable the permissions for ui
                        updateUiBasedOnPermissions()
                    }
                }
            }
        // filePickerLauncher inicializálása JSON fájlok kiválasztásához
        //JSON file picker launcher initialization for selecting JSON files
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            //Csak akkor folytatjuk ha az uri nem null(üres)
            //Only continue if the uri is not null(empty)
            if (uri != null) {
                Log.d("MainActivity", "Kiválasztott fájl URI: $uri")
                //itt hívjuk meg a JSON Olvasó függvényünket
                //here we call the JSON Reader function
                //val loadedQuestions = jsonLoaderSerializable.readJsonQuestion(uri) || régi a serializablé-hoz
                //It is an old code with the serializable loader
                val loadedQuestions = loadQuestionWithSelectedParser(uri)
                if (loadedQuestions.isNotEmpty()) {
                    questionList.clear()
                    questionList.addAll(loadedQuestions)
                    //Beállítjuk az indexet az első kérdésre
                    //Set the index to the first question
                    currentQuestionIndex = 0
                    //megjelenítjük az első kérdést
                    //show the first question
                    showQuestion()
                    //Üzeneteket adunk a felhasználó felé a file kezelésről
                    //Add messages to the user about the file management
                    Toast.makeText(
                        this, getString(
                            R.string.filePickerLauncherLoadedQuestionText,//Kérdések betöltve || Questions loaded
                            loadedQuestions.size
                        ), Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this, getString(
                            R.string.filepickerLauncherErrorText
                        ),//A kérdéseket nem sikerült betölteni || Failed to load the questions
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                //Ha a felhasználó véletlenül nem jelölt ki fájlt
                //If the user did not select a file
                Toast.makeText(
                    this, getString(
                        R.string.filepickerLauncherNofileText
                    ),//Nincs fájl kiválasztva || No file selected
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //A JSON választható betöltö módok függvénye
    //JSON depends on the available loading modes
    private fun loadQuestionWithSelectedParser(uri: Uri): List<QuestionAnswer> {
        //Olvassuk ki a mentett módott a SharedPerferences-ből.
        //Alapértelmezett lesz a 'serializable' mód ha nincs kiválasztva semmi.
        //Read the selected mode from the SharedPreferences.
        //The default mode will be 'serializable' if nothing is selected.
        val selectedMode = prefs.getString(JSON_LOADER_MODE, MODE_SERIALIZABLE)
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "JsonLoader of Selected mode: $selectedMode")
        }
        //A when segítségével listázuk a módokat
        //Use when to list the modes
        return when (selectedMode) {
            MODE_SERIALIZABLE -> jsonLoaderSerializable.readJsonQuestion(uri)
            MODE_MANUAL_JSON -> jsonLoader.readJsonQuestion(uri)
            MODE_GSON -> gsonJsonLoader.readJsonQuestion(uri)
            MODE_CPP -> cppJsonLoader.readJsonQuestion(uri)
            else -> {
                Log.w(
                    "MainActivity",
                    "Unknown JSON loader mode: '$selectedMode' using 'serializable'"
                )
                jsonLoaderSerializable.readJsonQuestion(uri)
            }
        }

    }

    //Az engedély ellenőrzése
    //Permission check
    private fun storageHasPermissions(): Boolean {
        /*val hasPerms: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasPerms = Environment.isExternalStorageManager()
        //Nem kell a return if miatt
        //No need because of the return if
        */
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val permissionRead =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            val permissionWrite =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            return permissionRead == PackageManager.PERMISSION_GRANTED && permissionWrite == PackageManager.PERMISSION_GRANTED
            /*hasPerms = permissionRead == PackageManager.PERMISSION_GRANTED && permissionWrite == PackageManager.PERMISSION_GRANTED
    } //a return if miatt nem kell mert alapból hivatkozunk rá
    //no need for return if because we reference it by default
    return hasPerms*/
        }
    }

    //Az engedélyKérés eredménye felülírjuk az onRequestPermissionsResult() függvényt
    //Overwrite the onRequestPermissionsResult() function with the permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CONSTATE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // A felhasználó MOST ADTA MEG az engedélyeket a felugró ablakban
                // The user has NOW GRANTED the permissions in the popup
                showPermissionGrantedToastOnce()
                // frissítsük az UI-t, miután megkaptuk az engedélyt
                // update the UI after receiving permission
                updateUiBasedOnPermissions()
            } else {
                //Írunk egy üzenetet a felhasználónak hogy kellenek az engedélyek
                //We write a message to the user that the permissions are needed
                Toast.makeText(
                    this,
                    getString(R.string.permissionRequest_ResultText2), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //A tárhely engedélyének az eseménykezelőja
    //Storage permission event handler
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android 11 és felette bekérjük az engedélyeket
            //Android 11 and above we ask for permissions
            if (!Environment.isExternalStorageManager()) {
                val intent =
                    Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                storagePermissionLauncher.launch(intent)
            }
            //itt nincs szükség az else ágra mert már meg vannak az engedélyek
            //
        } else {
            //Android 10 és alatta bekérjük az engedélyeket
            //Android 10 and below we ask for permissions
            val permissionsToRequest = mutableListOf<String>()
            //ellenörizük az olvasási engedélyeket
            //check for read permissions
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            //ellenőrizük az írási engedélyeket
            //check for write permissions
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            //ellenőrizük hogy megvannekl e az engedélyek
            //check if the permissions were granted
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    REQUEST_PERMISSION_CONSTATE
                )
            }
        }
    }

    //az UI frissítésére az engedélyek alapján
//UI update based on the permissions
    private fun updateUiBasedOnPermissions() {
        //ha vannak engedélyek megjelenítjük a fájlkezelő gombot
        //if there are permissions we display the file manager button
        if (storageHasPermissions()) {
            binding.JsonLoadingButton.isEnabled = true
            //mutatjuk a kérdéseket ha van engedély
            //show the questions if there are permissions
            showQuestion()
        } else {
            //ha nincsenek letiltjuk
            //if they are not there, we will block them
            binding.JsonLoadingButton.isEnabled = false
            //majd kérjük az negedélyeket ha nincsenek újból
            requestStoragePermission()
        }
    }

    //
    private fun updateButtonStatesForQuestion() {
        //ha a lista üres || if the lis is empty
        val isListEmpty = questionList.isEmpty()
        //akkor letiltunk pár gombot || if it is, we disable some buttons
        binding.CheckAnswerButton.isEnabled =
            !isListEmpty //válasz ellenőrzése gomb || Check answer button
        binding.ResetQuestionsButton.isEnabled =
            !isListEmpty// Kérdések ujraindítása gomb || Reset questions button
        binding.nextQuestionButton.isEnabled =
            !isListEmpty// Jobb navigációs gomb || Right navigation button
        binding.previousQuestionButton.isEnabled =
            !isListEmpty//Előző(Bal) navigációs gomb || Left navigation button
        //Ha nem üres a lista
        //itt csak a két gomb állapotát írjuk felül
        //Mert ha nem üres a lista a gombok engedélyezve lesznek
        //If the list is not empty
        //here we only overwrite the state of the two buttons
        //Because if the list is not empty the buttons will be enabled
        if (!isListEmpty) {
            binding.previousQuestionButton.isEnabled = currentQuestionIndex > 0
            binding.nextQuestionButton.isEnabled = currentQuestionIndex < questionList.size - 1
        }
    }

    // Egy toast üzenet a felhasználónak az engedélyekre ami egyszer futt le ha megvan
    //
    private fun showPermissionGrantedToastOnce() {
        if (!prefs.getBoolean(PREF_PERMISSION_TOAST_SHOWN, false)) {
            Toast.makeText(
                this,
                getString(R.string.checkStoragePermissionText1),
                Toast.LENGTH_SHORT
            ).show()
            prefs.edit { putBoolean(PREF_PERMISSION_TOAST_SHOWN, true) }
        }
    }

    //Megjeleníti a kérdést a jsonFájl indexe alapján
    //Shows the question based on the Json file index
    //A QuestionCounter és a QuestionOfText-hez
    private fun showQuestion(index: Int = currentQuestionIndex) {
        if (index >= 0 && index < questionList.size) {
            //frissítjük az indexet
            //update the index
            currentQuestionIndex = index
            /*questionAndAnswerInTheIndex == questionAndAnswerAtIndex*/
            val questionAndAnswerAtIndex = questionList[index]
            //betöltjük a kérdés mezőt a fájl indexe alapján
            //load the question field based on the file index
            binding.questionOfText.text = questionAndAnswerAtIndex.question
            //a válasz mezőt ürítjük(answerInput)
            //clear the answer field(answerInput)
            //binding.answerInput.setText("") de jobb a text.clear() kevesebb erőforrás
            //binding.answerInput.setText("") is better than text.clear() because it is less resource consuming
            binding.answerInput.text.clear()
            //visszajelzési mező űrítése(feedbackText)
            //feedback text clear(feedbackText)
            //A textViewnek nincs clear() függvénye
            //the textView has no clear() function
            binding.feedbackText.text = ""
            //Beállítjuk a kérdésSzámláló szövegét(QuestonCounter)
            //Set the question counter text
            binding.questionCounter.text = getString(
                R.string.questionCounter_format,
                currentQuestionIndex + 1,
                totalQuestionsCount
            )
            //Frisitjük a gombok állapotát
            //Update the button states
            updateButtonStatesForQuestion()
        } else if (questionList.isEmpty()) {//Ha üres a lista
            binding.questionOfText.text = getString(R.string.noQuestionsText)
            //binding.questionCounter.text = ""
            //nem ürítjük hanem beállítjuk az üres szöveget
            //we set the empty state text
            binding.questionCounter.text = getString(R.string.question_counter_empty_state)
            //a felhasználó válaszát is ürítjük
            //we clear the user answer
            binding.answerInput.text.clear()
            //a visszajelzési szöveget is ürítjük
            //we clear the feedback text
            binding.feedbackText.text = ""
            //Frisitjük a gombok állapotát mert üres a lista letiltunk néhányat
            //Update the button states because the list is empty we disable some buttons
            updateButtonStatesForQuestion()
        } else {
            Toast.makeText(
                this, getString(
                    R.string.invalid_question_Index
                ), Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    //A Gombok eseménykezelője következik.
    //Next The Buttons of Listener
    private fun setupButtonListeners() {
        //Elöző(Bal) kérdés gomb || Left navigation button
        binding.previousQuestionButton.setOnClickListener {
            //Csak akkor csinálunk bármit ha vannak kérdések
            //Only do something if there are questions
            if (questionList.isNotEmpty()) {
                if (currentQuestionIndex > 0) {
                    //Csökkentjük a kérdések indexét a visszalépéshez
                    //Decrease the question index for back navigation
                    currentQuestionIndex--
                    //megjelenítjük a kérdést
                    //show the question
                    showQuestion()
                } else {
                    //Ha az első kérdésnél vagyunk nem tudunk visszalépni
                    //if we are at the first question we cannot go back
                    Toast.makeText(
                        this, getString(
                            R.string.already_at_first_question
                        ), Toast.LENGTH_SHORT
                    ).show()
                }

            } else {
                //Amikor nincsenek kérdések betöltve
                //If there are no questions loaded
                //Üzenünk a felhasználónak
                //Notify the user
                //Ilyenkor nem tudunk navigálni
                //In this case we cannot navigate
                Toast.makeText(
                    this, getString(
                        R.string.no_questions_to_navigate
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            //Rendszerüzenet hogy használtuk a gombot
            //System message that we used the button
            if ((Log.isLoggable(TAG, Log.DEBUG))) {
                Log.d(
                    TAG,
                    "previousQuestionButton. clicked. " +
                            "Current index: $currentQuestionIndex"
                )
            }
        }
        //következő kérdés gomb || Right navigation button
        binding.nextQuestionButton.setOnClickListener {
            //Csak akkor csinálunk bármit ha vannak kérdések
            //Only do something if there are questions
            if (questionList.isNotEmpty()) {
                if (currentQuestionIndex < questionList.size - 1) {
                    //Nüveljük a kérdések indexét 1-el
                    //Increase the question index by 1
                    currentQuestionIndex++
                    //megjelenítjük a kérdést
                    //show the question
                    showQuestion()
                } else {
                    //Ha az utolsó kérdésnél vagyunk nem tudunk navigálni
                    //if we are at the last question we cannot navigate
                    Toast.makeText(
                        this, getString(
                            R.string.already_at_last_question
                        ), Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                //Amikor nincsenek kérdések betöltve
                //If there are no questions loaded
                //Üzenünk a felhasználónak
                //Notify the user
                //Ilyenkor nem tudunk navigálni
                //In this case we cannot navigate
                Toast.makeText(
                    this, getString(
                        R.string.no_questions_to_navigate
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            //Rendszerüzenet hogy használtuk a gombot
            //System message that we used the button
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(
                    TAG, "nextQuestionButton. clicked. " +
                            "Current index: $currentQuestionIndex"
                )
            }
        }
        //Válaszok ellenőrzése gomb
        //Check Answer Button
        binding.CheckAnswerButton.setOnClickListener {
            //ellenőrizük van -e egyáltalán aktuális kérdés betöltve
            //check if there are any current questions loaded
            if (questionList.isNotEmpty() && currentQuestionIndex >= 0 &&
                currentQuestionIndex < questionList.size
            ) {
                //A felhasználó válaszát kisbetűssé teszük és eltávolítjuk a felesleges szóközöket
                //We lowercase the user's response and remove unnecessary spaces
                val userAnswer = binding.answerInput.text.toString().trim().lowercase()
                //Ellenőrizük hogy a felhasználó válasza szerepel-e a helyes válaszok között
                //És kikeressük az indexben hogy hol szerepel
                //Check if the user's answer is among the correct answers
                //And let's look in the index to see where it is listed.
                val currentQuestionAnswer = questionList[currentQuestionIndex]
                //A helyes válaszokat és kisebtűssé teszük + eltávolítjuk a felesleges szóközöket
                //We lowercase the correct answers and remove unnecessary spaces
                /*any az egy olyan kotlin függvény ami ellenőrzi a listában hogy az adott érték szerepel-e
                ha igen true-val tér vissza ez javában az anyMatch->
                any is a kotlin function that checks if the given value is in the list
                if so it returns true this is basically anyMatch->
                * */
                //Ellenőrizzük hogy, a felhasználó válasza szerepel-e a helyes válaszok között.
                //We check whether the user's answer is among the correct answers
                val isAnswerCorrect = currentQuestionAnswer.rightAnswers.any { correctAnswer ->
                    correctAnswer.trim().lowercase() == userAnswer
                }
                if (isAnswerCorrect) {
                    binding.feedbackText.text = getString(R.string.rightAnswer)
                    //A helyes válasz színe zöld lesz
                    //The correct answer color will be green
                    binding.feedbackText.setTextColor(
                        ContextCompat.getColor(
                            this, android.R.color.holo_green_dark
                        )
                    )
                    //Megjelöljük a választ megválaszoltként ha helyes volt
                    //Mark the answer as answered correctly
                    currentQuestionAnswer.answered = true
                    //Hozzáadunk egy kis késleltetést hogy a felhasználó lássa
                    // mielött megjelenik a következő kérdés
                    //We add a little delay for the user to see
                    // before the next question appears
                    binding.answerInput.postDelayed(
                        {
                            currentQuestionIndex++
                            if (currentQuestionIndex < questionList.size) {
                                showQuestion()
                            } else {
                                //Ha minden kérdésre válaszoltunk jelezük a felhasználónak
                                //We will notify the user when we have answered all questions.
                                binding.questionOfText.text = getString(R.string.all_answered)
                                //És minden új kérdésnél kiűrítjük a válaszmezőt
                                //And for every new question, we clear the answer field
                                //Mert nem akarjuk hogy látszodjon az elözö válasz eredménye
                                //Because we don't want the result of the previous answer to be visible
                                //mainUI.answerInput.setText("")
                                binding.answerInput.text.clear()
                                //és töröljük a visszajelzést is
                                //And we clear the feedback text
                                binding.feedbackText.text = ""
                            }
                        },//it is important , comma || fontos a vessző
                        500 //ide jön a késleltetés miliszekundumban || here is the delay in milliseconds
                    )
                } else {
                    //Hibás válasz esetén kiírjuk a helyes válaszokat
                    //In case of incorrect answers, we will print the correct answers
                    val correctAnswerText =
                        currentQuestionAnswer.rightAnswers.joinToString(", ")
                        { it.trim() }
                    binding.feedbackText.text =
                        getString(
                            R.string.wrong_answer_with_correct_answer,
                            correctAnswerText
                        )
                    //beállítjuk a színt pirosra hibás válasz esetén
                    //set the color to red for incorrect answers
                    binding.feedbackText.setTextColor(
                        ContextCompat.getColor(
                            this, android.R.color.holo_red_dark
                        )
                    )
                }
            } else if (questionList.isEmpty()) {
                //Ha nincsenek kérdések betöltve
                //if there are no questions loaded
                Toast.makeText(
                    this,
                    getString(R.string.no_questions_loaded_for_checking),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //ha hiba történt a válasz ellenőrzésénél
                //If an error occurs during the answer checking
                Toast.makeText(
                    this, getString(
                        R.string.error_checking_answer
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        //A kérdések ujraindítása gomb
        //The Reset Questions Button
        binding.ResetQuestionsButton.setOnClickListener {
            //Most visszaállítjuk az összes kérdés "answered""válaszolva állapotát
            //Reset all questions to their "answered" state
            if (questionList.isNotEmpty()) {
                /*A forEach minden elemen végigiterált(megy)a listán
                * it.answered = false == Nem válaszolt állapotot állítja be az összes kérdésre
                * forEach iterates over each element in the list
                * it.answered = false == Sets all questions to an unanswered state
                **/
                questionList.forEach { it.answered = false }
                //visszaállítjuk a kérdések indexét az elsőre
                //Reset the question index to the first question
                currentQuestionIndex = 0
                //Megjelenítjük és ürítjuk a kérédsmezőt a függvény által
                //We display and clear the query field using the function
                showQuestion()
                /*ezért ezek feleslegesek
                    binding.answerInput.text.clear()
                    binding.feedbackText.text = ""
                 */
                //visszajelzés a felhasználónak
                //feedback to the user
                Toast.makeText(
                    this, getString(
                        //Amikor újraindítottuk a kérdéseket
                        //When we reset the questions
                        R.string.questions_restarted
                    ), Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this, getString(
                        //Amikor nincs mit újraindítani
                        //When there is nothing to restart
                        R.string.no_questions_to_restart
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        //A Json fájl betöltése gomb
        //The Json Loading Button
        /*Ne felejtsd el már az updateUiBasedOnPermissions() letiltja a Json gombot
        * ha nincsenek meg az enegedélyek és meg van hívva az onCreate() függvényben!
        * Don't forget updateUiBasedOnPermissions() disables the Json button
        * if the permissions are not present and it is called in the onCreate() function!
        */
        binding.JsonLoadingButton.setOnClickListener {
            //Elinditjuk a filepickerlaunchert
            //Start the file picker launcher
            // A "application/Json" MIME típus megadja, hogy milyen típusú fájlokat keressen.
            //The "application/Json" MIME type tells the launcher to look for JSON files
            filePickerLauncher.launch("application/json")
            //Rendszerüzenet hogy használtuk a gombot
            //System message that we used the button
            Log.d(
                "MainActivity",
                "jsonLoadingButton clicked," +
                        " launching file picker for JSON files."
            )
        }
        //A beállítások gomb eseménykezelője
        //The Settings Button EventHandler
        binding.SettingsButton.setOnClickListener {
            /*intent tehát a szándékal hívunk meg egy másik Activity képernyő tevékenységet
            this(mainActivity-ből), SettingsActivity-t hívjuk meg a startActivity() függvényel
            A Settings Activity egy másik képernyő más elrendezésel
            intent tehát a szándékal hívunk meg egy másik Activity képernyő tevékenységet
            this(mainActivity-ből), SettingsActivity-t hívjuk meg a startActivity() függvényel
            A Settings Activity egy másik képernyő más elrendezésel
             */
            val intent = Intent(
                this,
                SettingsActivity::class.java
            )
            startActivity(intent)
            //Rendszerüzenet hogy használtuk a gombot
            //System message that we used the button
            Log.d(
                "MainActivity",
                "settingsButton. clicked."
            )
        }
        //A Kérdések létrehozása Gomb eseménykezelője
        //Create Questions Button event handler
        binding.CreateQuestionsButton.setOnClickListener {
            val intent = Intent(
                this,
                CreateQuestionActivity::class.java
            )
            startActivity(intent)
            //Rendszerüzenet hogy használtuk a gombot
            //System message that we used the button
            Log.d(
                "MainActivity",
                "createQuestionsButton clicked, attempting to launch CreateQuestionActivity."
            )
        }
        // Kérdések Szerkesztése gomb
        // Edit Questions Button
        binding.EditQuestionsButton.setOnClickListener {
            //Deklatáljuk a szándékot(Intent)
            val intent = Intent(
                this,
                EditQuestionActivity::class.java
            )
            startActivity(intent)
            //Rendszerüzenet hogy használtuk a gombot
            //System message that we used the button
            Log.d(
                "MainActivity",
                "editQuestionsButton clicked, attempting to launch EditQuestionActivity."
            )
        }
    }
}

