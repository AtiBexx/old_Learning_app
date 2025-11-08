package com.attibexx.old_learning_app

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Log.isLoggable
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.attibexx.old_learning_app.AppSettingsKeys.PREFS_FILE_NAME
import com.attibexx.old_learning_app.AppSettingsKeys.ZOOM_ENABLED
import com.attibexx.old_learning_app.databinding.ActivityCreateQuestionBinding
import com.attibexx.old_learning_app.json.JsonProcessorFactory


class CreateQuestionActivity : AppCompatActivity() {
    // Question input mode hozzáadása
    // A kérdés és válaszoknak amentésére
    // Add Question input mode
    // To save questions and answers
    private var isQuestionInputMode = true

    // aktuálisKérdésSzövegének az incializálása(üres)
    // initialize the currentQuestionText (empty)
    //A kérdés ideiglenes elmentése || Temporarily store the question
    private var currentQuestionText = ""

    // Lista a válaszoknak || List for answers
    private var currentAnswers = mutableListOf<String>()

    // Lista a komplett kérdés-válasz pároknak
    // List for complete question-answer pairs
    private val questionAnswerPairs =
        mutableListOf<QuestionAnswer>()

    // A fájlkezelőhöz a launcher-hez a fájl létrehozásához
    // a SAF(Storage Access Framework) segítségével.
    // For the file manager, for the launcher, to create the file
    // using SAF(Storage Access Framework).
    private lateinit var fileSaveLauncher: ActivityResultLauncher<String>

    //Az a változó ami ideglenesen tárolja a JSON string-eit.
    //The variable that temporarily stores JSON strings.
    private var jsonStringToSave: String = ""

    // A JsonSaver(Json fájl mentése) példányosítása
    // Instance of JsonSaver(Json file saving)
    /*A 'lazy' kulcsszó azt jelenti csak akkor jön létre ha használod
    * erőforrás sporolás egyfajta késleltetés.
    *'lazy' keyword means it is created only when you use it
    *resource sparing is a type of delay.*/
    // private val jsonSaver by lazy { JsonSaver(this) }

    // binding példányosítása || Instance of binding
    private lateinit var binding: ActivityCreateQuestionBinding

    // Prefs(központi beállítások) példányosítása || Instance of prefs
    private val prefs by lazy {
        getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE) }

    // Companion object elsősorban a válaszhtató
    // Json fájl létrehozása módokhoz. + a loggoláshoz
    // Companion object is primarily for responsive
    // Json file creation methods + logging for debug.
    companion object {
        // Logoláshoz használt címke
        // Label used for logging
        private const val TAG = "CreateQuestionActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Beállítjuk hogy ne lógjon bele a képernyő a gombokhoz és a fenti eszköztárba
        //We set the screen to not hang in the buttons and the toolbar above
        enableEdgeToEdge()

        //A képernyő fellépítése a Layout alapján
        //The screen fills the layout
        binding = ActivityCreateQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A gombok eseménykezelőinek meghívása
        // Call the button event handlers
        setupButtonListeners()

        // A Fájlkezelés incializálása
        // Intialize the Filemanager
         fileSaveLauncher = registerForActivityResult(
             // Beállítjuk csak json kiterjesztésű fájlokat mentünk
             // We set to save only files with json extension
             // És majd a felhasználó beállítja a mentési helyét a fájlelválasztó segítségével
             // And then the user sets the save location using the file picker
             ActivityResultContracts.CreateDocument("application/json")
         ) { uri: Uri? ->
             // ez a blokk akkor fut le ha a felhasználó kiválasztott egy mentési helyet
             // this block runs when the user selects a save location
             if (uri != null) {
                 // Ha a felhasználó kiválasztotta a mentési helyett
                 // elmentjük a json tartalmát.
                 // If the user selected a save location
                 // save the json content
                 saveStringToFile(uri, jsonStringToSave)
             } else {
                 //Ha a felhasználó a mégse gomra nyomott a fájlkezelőben
                 Toast.makeText(
                     this,
                     getString(R.string.Create_Question_Save_Cancel_Json), Toast.LENGTH_SHORT
                 ).show()
             }
         }

            // A széltől-szélig kinézet helyes kezelése
            // Correct handling of edge-to-edge layout
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // ÁllapotVáltozások kezelése a képernyő elforgatásakor
        // Handling state changes when rotating the screen
        override fun onConfigurationChanged(newConfig: Configuration) {
            super.onConfigurationChanged(newConfig)
            if (isLoggable(TAG, Log.DEBUG)) {
                Log.d(
                    TAG,
                    "onConfigurationChanged: New orientation: ${newConfig.orientation}"
                )
            }
        }

    // Beállítások újraolvasására onResume() függvény.
    // onResume() function to reread settings.
    override fun onResume() {
        super.onResume()

        // Kiolvassuk a mentett beállítást.
        val isZoomAllowed = prefs.getBoolean(
            ZOOM_ENABLED, false)

        binding.zoomContainer.isZoomEnabled = isZoomAllowed
    }
        /**
        *Függvény az Ui Frisítésére
        *Function to update the Ui
        */
        private fun updateUI() {
            // Üzeneteket írunk a felhasználónak az állapotok változásairól
            // Write messages to the user about the state changes
            // ha beviteli módban vagyunk
            // if on input mode
            if (isQuestionInputMode) {
                binding.answerQuestionInputView.text =
                    getString(
                        R.string.create_question_status_enter_question
                    )
                binding.nextButtonInputAnswerQuestion.text = getString(
                    R.string.create_question_button_next_to_answers
                )
                binding.answerInput.hint = getString(
                    R.string.create_question_hint_enter_question
                )
                binding.answerInput.setText("") // Mindig töröljük az inputot || Always clear the input
            } else {
                // Ha nem vagyunk beviteli módban
                // if this not on input mode
                binding.answerQuestionInputView.text = getString(
                    R.string.create_question_status_enter_answers_for,
                    currentQuestionText
                )
                binding.nextButtonInputAnswerQuestion.text = getString(
                    R.string.create_question_button_save_answers_and_next_q
                )
                binding.answerInput.hint = getString(
                    R.string.create_question_hint_enter_answers_csv
                )
                binding.answerInput.setText("") // Mindig töröljük az inputot || Always clear the input
            }
        }
        /**
        *A Json fájl-ba író függvényünk
        *Our function that writes to the Json file
         */
        private fun saveStringToFile(uri: Uri, content: String) {
            try {
                // A contentResolver segítségével megnyitunk egy kimenti csatornát(uri-t)
                // majd a végen lezárjuk azaz automatikusan lezárul(use blokk).
                // We open an output channel (uri) using the contentResolver
                // then we close it at the end, i.e. it closes automatically (use block).
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    //A szöveget bájtokká alakítjuk és beleírjuk a fájlba.
                    //We convert the text to bytes and write it to the file.
                    outputStream.write(content.toByteArray())
                }
                // Írunk egy üzenetett a mentés sikeres volt üzenettel
                // We write a message that the save
                Toast.makeText(
                    this,
                    getString(R.string.create_question_json_file_created),
                    Toast.LENGTH_SHORT
                ).show()
                // A sikeres mentés után takarítunk
                // We clean up after a successful save
                questionAnswerPairs.clear()
                currentQuestionText = ""
                currentAnswers.clear()
                isQuestionInputMode =
                    true // vissza QuestioninputMódba || back to the QuestionInputMode
                jsonStringToSave = ""
                updateUI()
            } catch (e: Exception) {
                // Ha bármilyen hiba történik azt loggoljuk és
                // Toast üzenetben jelezzük a felhasználónak is.
                if (isLoggable(TAG, Log.ERROR)) {
                    Log.e(TAG, "Error saving to file:", e)
                    Toast.makeText(
                        this,
                        getString(
                            R.string.create_question_error_creating_json
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    /**
     * Törli az összes eddig bevitt kérdés-válasz párt és a félbehagyott adatokat.
     * Visszaállítja az UI-t az alaphelyzetbe.
     * Deletes all saved question-answer pairs and resets the input.
     * Returns the UI to the default state.
     */
    private fun resetAllData() {
        // kiüritjük az összes listát és az ideglenes változót.
        // all lists and temporary variables are cleared(reset)
        questionAnswerPairs.clear()
        currentAnswers.clear()
        currentQuestionText = ""
        jsonStringToSave = ""

        // visszaállítjuk a beviteli módott a legelejére
        // reset the input mode to the very beginning
        isQuestionInputMode = true

        // frisitjük az ui-t(Felhasználói felületet)
        // we update the ui (User Interface)
        updateUI()

        // Írunk egy rendszerüzenetett
        // We write a system message
        if (isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "All data cleared," +
                    "all question data has been reset by the user.")
        }

        // Visszajelzünk a flhasználónak a sikeres törlésről(Toast-üzenet)
        // We will notify the user about the successful deletion (Toast message)
        Toast.makeText(
            this,
            getString(R.string.create_question_all_data_cleared_toast),
            Toast.LENGTH_SHORT
        ).show()

    }
        /**
        *A Gombok eseménykezelője
        *The Button of Listeners
        */
        private fun setupButtonListeners() {
            // Következö Gomb kérdés és válasz bevitele
            // Next Question and Answer Input Button
            binding.nextButtonInputAnswerQuestion.setOnClickListener {
                val inputText = binding.answerInput.text.toString().trim()
                // Ha a beviteli mező űres és a felhasználó menteni akar
                // akkor üzenünk neki hogy,üres és kilépünk az onClickListenerből !
                // If the input field is empty and the user wants to save it,
                // then we tell it that it is empty and exit the onClickListener
                if (inputText.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.create_question_please_enter_something),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Kilépünk az OnClickListenerből a további feldolgozás elkerülése érdekében
                    // Exiting the OnClickListener to avoid further processing
                    return@setOnClickListener
                }
                // Kérdés beviteli mód
                // question input mode
                if (isQuestionInputMode) {
                    // A beírt kérdés ideiglenes elmentése
                    // Temporarily store the entered question
                    currentQuestionText = inputText
                    // Átmegyünk válasz beviteli módba
                    // Switch to answer input mode
                    // Mivel isQuestionInputMode false ezért az else ág fog lefutni
                    // Because isQuestionInputMode is false the else branch will run
                    isQuestionInputMode = false
                    //Az Ui frisítése || Update Ui
                    updateUI()
                } else {
                    // itt fogjuk elmenteni a válaszokat beviteli módban
                    // this is where we will save the answers in input mode
                    // A válaszokat vesszővel elválasztva fogjuk tenni,
                    // majd eltávolítjuk a felesleges szóközt és kisbetűsítjük
                    val answersArray = inputText.split(",")
                        .map { it.trim().lowercase() }
                    // Töröljük az elözö válaszokat ha voltak
                    currentAnswers.clear()
                    currentAnswers.addAll(answersArray)
                    // Mentsük el a kérdés-válasz párt ha mindkettő érvényes
                    // Save the question-answer pair if both are valid
                    if (currentQuestionText.isNotBlank() && currentAnswers.isNotEmpty()) {
                        //Hozzáadjuk az értékeket aQuestionAnswerPairs-hez
                        //Add the values to the questionAnswerPairs
                        questionAnswerPairs.add(
                            QuestionAnswer(
                                // az eddig beirtkérdések mentése(kérdések másolatta)
                                // save the questions you have entered so far (copy of questions)
                                currentQuestionText,
                                // Az eddig beírt válaszok arrayLista mentése(a válaszok arraylistájának a másolatta)
                                // Save the arrayList of answers entered so far (a copy of the arraylist of answers)
                                ArrayList(currentAnswers),
                                // a boolean answered false a Json logikához
                                // boolean answered false for Json logic
                                answered = false
                            )
                        )
                        // Üzenünk a felhasználónak
                        // Message the user
                        // A kérdés párok elmentve
                        // Question pairs saved
                        Toast.makeText(
                            this,
                            getString(
                                R.string.create_question_answer_pair_saved,
                                questionAnswerPairs.size
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        // isQuestionInputMode visszarakujuk true-ra az updateUi miatt
                        // isQuestionInputMode is set back to true due to updateUi
                        isQuestionInputMode = true
                        // Ürítjük a bevitelt következő válaszokhoz
                        // Clear the input for the next
                        currentAnswers.clear()
                        currentQuestionText = ""
                        //Az Ui frisítése || Update Ui
                        updateUI()
                    } else {
                        // Megüzenjük a felhasználónak hogy a kérések vagy a válaszok hiányosak a mentéshez
                        // We notify the user that requests or responses are incomplete for saving
                        Toast.makeText(
                            this,
                            getString(R.string.create_question_error_incomplete_pair),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            /*
            str = string
            str.isEmpty()      // üres-e
            str.isNotEmpty()   // nem üres
            str.isBlank()      // üres vagy szóköz
            str.isNotBlank()   // nem üres, nem szóköz
    */
            //Mentés fájlba gomb
            // Save file button
            binding.saveFileButton.setOnClickListener {
                // ellenőrizük van-e valami beírva az elmentéshez
                // Check if something is written for saving
                if (questionAnswerPairs.isEmpty() && currentQuestionText.isBlank()
                    && binding.answerInput.text.toString().isBlank()
                ) {
                    // Most jelzünk a felhasználónak egy üzenettel hogy üres
                    Toast.makeText(
                        this,
                        getString(R.string.create_question_nothing_to_save),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Kilépünk az onClickListenerből
                    // Exiting the OnClickListener
                    return@setOnClickListener
                }
                /*!isQuestionInputMode = nem vagyunk questionInputMódban
        * ! logikai negálás = ellentétte az eredetinek
        * */
                // Ha már beírtuk a kérdést rányomtunk a next-re de
                // a választ még nem írtuk be
                // Once we have entered the question, we click on next but
                // we haven't written the answer yet

                if (!isQuestionInputMode && currentQuestionText.isNotBlank()) {
                    val lastAnswerTetxt = binding.answerInput.text.toString().trim()
                    if (lastAnswerTetxt.isNotEmpty()) {
                        currentAnswers =
                            lastAnswerTetxt.split(",").map { it.trim().lowercase() }
                                .toMutableList()

                        // csak akkor adjuk hozzá a kérdésVálasz listához az elemeket
                        // ha nem üresek
                        // Only add the elements for the questionAnswerPairs list,
                        // if they are not empty

                        // csak akkor adjuk hozzá a kérdésVálasz listához az elemeket
                        // ha nem üresek
                        // Only add the elements for the questionAnswerPairs list,
                        // if they are not empty
                        if (currentAnswers.isNotEmpty()) {
                            questionAnswerPairs.add(
                                QuestionAnswer(
                                    question = currentQuestionText,
                                    rightAnswers = ArrayList(currentAnswers),
                                    answered = false
                                )
                            )
                            // Az ui visszaállítása mentés után
                            // Update ui after saving
                            // Ürítjük a kérdés listát
                            currentQuestionText = ""
                            // űrítjük a válaszlistát
                            currentAnswers.clear()
                            // Visszalépünk kérdés-válasz input(Beviteli) módba
                            // Back to the Questions and Answers input mode
                            isQuestionInputMode = true
                            // Frisitjük az ui-t az UpdateUi függvényünkel
                            // Update the ui with the UpdateUi function
                            updateUI()
                            // Üzenünk a felhasználónak az utólso kérdés-válasz párt is hozzáadta
                            // Message to the user the last question-answer pair has been added
                            Toast.makeText(
                                this, getString(
                                    R.string.create_question_last_pending_pair_saved_toast
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                // Ha volt egy kérdés de nem adtunk hozzá választ
                // If there was a question but no answer was added
                if (questionAnswerPairs.isEmpty()) {
                    // Ha a félbehagyott pár kezelése után is üres a lista,
                    // akkor ezt az üzenetet küldjük.
                    // If the list is still empty after handling the abandoned pair,
                    // then we send this message.
                    Toast.makeText(
                        this,
                        getString(
                            R.string.create_question_please_provide_answers_for_current_q_before_saving
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    // kilépünk az onClickListenerből
                    // Exiting the OnClickListener
                    return@setOnClickListener
                }
                // Ha kérdés Módban vannak már elmentett párok de az utolsó
                // kérdés válaszaira még nem nyomta meg a felhasználó a
                // következö(kérdés és válasz bevitele) gombot hanem
                // rányomot a mentésre
                // If in question mode there are already saved pairs
                // but the user has not pressed the next button for
                // the answers to the last question, but has pressed save
                else if (isQuestionInputMode && binding.answerInput.text.toString()
                        .isNotBlank() && questionAnswerPairs.isNotEmpty()
                ) {
                    Toast.makeText(
                        this,
                        getString(R.string.createquestion_press_next_to_add_current_question_first),
                        Toast.LENGTH_LONG
                    ).show()
                    // kilépünk a setOnClickListenerből
                    // Exiting the OnClickListener
                    return@setOnClickListener
                }
                // Ha kérdés módban, írtunk valamit a kérdés mezőbe,
                // de nincsenek korábbi KérdésVálasz párok
                // és nem nyomott tovább következö(kérdés és válasz bevitele) gombra
                // a válaszok megdadásához
                // If in question mode, you typed something in the question field
                // but there are no previous QuestionAnswer pairs
                // and you did not press the next (enter question and answer) button
                // to enter the answers
                else if (isQuestionInputMode && binding.answerInput.text.toString()
                        .isNotBlank() && questionAnswerPairs.isEmpty()
                ) {
                    // Írunk a felhasználónak egy üzenetett róla
                    // We write a message to the user about it
                    Toast.makeText(
                        this,
                        getString(R.string.create_question_add_answers_for_this_question_too),
                        Toast.LENGTH_LONG
                    ).show()
                    // Kilépünk az onClickListenerből
                    // Exiting the OnClickListener
                    return@setOnClickListener
                }
                // Ellenőrizük van-e mit mentenni a listában
                // Check if there is something to save in the list
                if (questionAnswerPairs.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.create_question_nothing_to_save_final),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Kilépünk az onClickListenerből
                    // Exiting the OnClickListener
                    return@setOnClickListener
                }
                // Json fájl létrehozása
                // Create a JSON file
                // Használjuk a JsonProcessFactoryt
                // Use the JsonProcessFactory
                val saverMode = JsonProcessorFactory.createSaver(this)
                val jsonString = saverMode.createJsonString(questionAnswerPairs)

                // Ellenőrizzük, hogy a JSON létrehozása sikeres volt-e
                // Check if JSON creation was successful
                if (jsonString != null) {
                    if (isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "JSON file created successfully: \n$jsonString")
                    }
                    Toast.makeText(
                        this,
                        getString(
                            R.string.create_question_json_file_created
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Mentsük el a generált stringet a jsonStringToSave változóba
                    // Save the generated string to the jsonStringToSave variable
                    jsonStringToSave = jsonString
                    // Indítsuk el a "Mentés másként..." ablakot
                    // egy alapértelmezett fájlnévvel amit a felhasználó átírhat.
                    fileSaveLauncher.launch("questions.json")
                } else {
                    // Ha a Json fájl létrehozása sikertelen volt(null-t adott vissza)
                    // If the JSON file creation was unsuccessful(returned null)
                    Toast.makeText(
                        this,
                        getString(
                            R.string.create_question_error_creating_json
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            // A létrehozott Json adatok tőrlésének a gombjának az eseménykezekője
            // Event handler for the button to clear the generated Json data
            binding.allDeleteButton.setOnClickListener {

                // Először ellenőrizzük, hogy van-e egyáltalán mit törölni.
                // Ha minden üres, felesleges felugró ablakot mutatni.
                // First, let's check if there is anything to delete at all.
                // If everything is empty, there is no need to show a pop-up window.
                if (questionAnswerPairs.isEmpty() && currentQuestionText.isBlank()
                    && binding.answerInput.text.toString().isBlank()
                ) {
                    Toast.makeText(
                        this,
                        getString(R.string.create_question_nothing_to_delete),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                // Ha van mit törölni, törlünk és
                // mutassunk egy megerősítő párbeszédablakot
                // az androidx.appcompat.app.AlertDialog segítségével.
                // If there is something to delete, delete it and
                // show a confirmation dialog using the androidx.appcompat.app.AlertDialog.
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.create_question_delete_confirmation_title))
                    .setMessage(getString(R.string.create_question_delete_confirmation_message))

                // A tőrlés gomb a párbeszédablakban
                // The delete button in the confirmation dialog
                .setPositiveButton(getString(R.string.create_question_delete_confirmation_positive_button)) { _, _ ->
                    // Meghívjuk a tőrlés függvényt
                    // Call the delete function
                    resetAllData()
                }
                // A mégse gomb a párbeszédablakban
                // The Cancel button in the confirmation dialog
                    .setNegativeButton(getString(R.string.create_question_delete_confirmation_negative_button),null)
                    // Adjunk hozzá egy figyelmeztető ikont
                    // Add a warning icon
                    .setIcon(R.drawable.baseline_warning_24)
                    // Jelenítsük meg a párbeszédablakot
                    // Show the confirmation dialog
                    .show()
            }
            // Vissza a főmenübe gomb eseménykezelője
            // The Back to Main Menu button event handler
            binding.backToTheMainMenuButton.setOnClickListener {
                finish()
                // A finish() függvény
                // Bezárja az aktuális Activity-t és visszatér az előzőhöz a főmenübe
                //The finish() function
                // Closes the current Activity and returns to the main menu
            }
        }
    }








