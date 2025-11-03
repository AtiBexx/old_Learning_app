package com.attibexx.old_learning_app

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Log.isLoggable
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.attibexx.old_learning_app.databinding.ActivityEditQuestionBinding
import com.attibexx.old_learning_app.json.JsonProcessorFactory
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException


class EditQuestionActivity : AppCompatActivity() {

    companion object {
        // Logoláshoz használt címke
        // Label used for logging
        private const val TAG = "EditQuestionActivity"
    }

    // Eltároljuk a szerkesztett fájl uri-ját
    // Store the edited file uri
    private var currentFileUri: android.net.Uri? = null


    // binding példányosítása || Instance of binding
    private lateinit var binding: ActivityEditQuestionBinding

    // A Fájlelválasztó pédányosítása || Instance of the file picker launcher
    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        // Ez a blokk akkor gut le ha a felhasználó kiválasztott egy fájlt
        // This block will run if the user has selected a file
        if (uri != null) {
            // Beolvassuk a kiválasztott fájlt
            // Read the selected file
            readTextFromFile(uri)
        }
    }

    // A fájl létrehozásához (mentéshez) használt launcher
    // Launcher used to create a file
    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) {
        // Ez a blokk akkor fut le ha a felhasználó kiválasztott
        // egy mentési helyet és egy fájl nevet a mentéshez.
        // This block will run if the user has selected a directory
        // and a file name for saving.
            uri: android.net.Uri? ->
        // Ha a uri nem üres
        // If the uri is not empty
        //uri?.let { writeTextToFile(it) }
        if (uri != null) {
            writeTextToFile(uri)
        }
    }

    // Verem az előzmények tárolására a visszavonás funkcióhoz.
    // Stack for storing history for the undo functionality.
    /*private val historyStack = Stack<String>()*/
    private val historyStack = ArrayDeque<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Beállítjuk hogy ne lógjon bele a képernyő a gombokhoz és a fenti eszköztárba
        //We set the screen to not hang in the buttons and the toolbar above
        enableEdgeToEdge()

        //A képernyő fellépítése a Layout alapján
        //The screen fills the layout
        binding = ActivityEditQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // A széltől-szélig kinézet helyes kezelése
        // Correct handling of edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // A codeView függvény
        // The CodeView function
        setupCodeView()

        // A gombok eseménykezelőinek a függvénye
        // Call the button event handlers
        setupButtonListeners()
    }

    // Incializáljuk a codeView-et
    // Initialize the codeView
    private fun setupCodeView() {
        // Ez a sor eltávolitja a codeview alsó vonalát
        // és a miénket fogjahasználni a texviewbordert
        // This line will remove the bottom line of the codeview
        // and use ours for the texviewborder
        binding.codeView.background = ContextCompat.getDrawable(
            this, R.drawable.textviewborder
        )

        // Sorszámozás bekapcsolása és stílusának beállítása
        // Enable line numbers and set the style
        binding.codeView.setEnableLineNumber(true)
        binding.codeView.setLineNumberTextColor(Color.GRAY)
        binding.codeView.setLineNumberTextSize(30f)

        // JSON minták beállítása (egyszer, induláskor)
        val patterns = HashMap<java.util.regex.Pattern, Int>()
        // Ez a sor a dokumentációból származik a JSON kiemeléshez
        patterns[java.util.regex.Pattern.compile(
            "\"(.*?)\""
        )] = "#A3BE8C".toColorInt() // Zöld szín stringekhez || Green color for Strings
        binding.codeView.setSyntaxPatternsMap(patterns)

        // hozzáadunk egy TextWatchert hogy figyeljük a szöveg változásait.
        // Add a TextWatcher to watch for text changes.
        binding.codeView.addTextChangedListener(object : TextWatcher {
            private var beforeText: String? = null

            // ez a metódus fut le mielőtt a szöveg megváltozna
            // this method runs before the text changes
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Elmentjük a szöveg aktuális állapotát a változás ELŐTT.
                // We save the current state of the text BEFORE the change.
                beforeText = s.toString()
            }

            // Ez a metódus fut le, miközben a szöveg változik.
            // This method runs while the text is changing.
            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            // Ez a metódus fut le miután a szöveg megváltozott
            // This methods runs after the text has changed.
            override fun afterTextChanged(s: Editable?) {

                // A változás után a beforeText-et beteszük a verembe.
                //
                /*if (beforeText != null) {
                    //historyStack.push(beforeText)
                    historyStack.addLast(beforeText!!)
                }*/
                beforeText?.let {
                    historyStack.addLast(it)
                }
            }
        })
    }
    // Létrehozuk a fájlBeolvasó függvényünket
    // Let's create our fileReader function
    /**
     * Beolvassa egy adott Uri-n keresztül elérhető fájl szöveges tartalmát.
     * Reads the text content of a file accessible via a given Uri.
     */
    private fun readTextFromFile(uri: android.net.Uri) {

        try {
            val loader = JsonProcessorFactory.create(this)
            val questionList: List<QuestionAnswer> = loader.readJsonQuestion(uri)

            // Ha a betöltés sikertelen volt
            // (üres listát adott vissza), jelezzük.
            // If the load was unsuccessful
            // (returned an empty list), we will notify you.
            if (questionList.isEmpty()) {
                Toast.makeText(this, getString(R.string.jsonParseError1), Toast.LENGTH_LONG).show()
                return
            }

            // Most kérj egy MENTŐT a gyárunktól. Ezt arra használjuk,
            // hogy az adat-objektumokat szépen formázott, olvasható szöveggé alakítsuk vissza.
            // Now ask our factory for a RESCUE. We use this to
            // convert the data objects back into nicely formatted, readable text.
            val saver = JsonProcessorFactory.createSaver(this)
            val formattedJsonString = saver.createJsonString(
                questionList
            )

            // Beállítjuk a formázott szöveget a CodeView-ba.
            // We set the formatted text in CodeView.
            if (formattedJsonString != null) {
                binding.codeView.setText(formattedJsonString)
            } else {
                // Ha a visszaalakítás valamiért sikertelen, jelezzük.
                // If the conversion fails for some reason, we will let you know.
                Toast.makeText(this, getString(R.string.beautify_invalid_json), Toast.LENGTH_LONG)
                    .show()
                return
            }

            // A szintaxis és a sorszám frisítésse
            // Refresh the syntax and line numbers
            refreshCodeView()

            // Fontos megnyitás után eltároljuk az URI-t
            // Important: We store the URI after opening it
            this.currentFileUri = uri

            // Írunk a felhasználónak egy üzenetett
            // hogy, sikeress volt a fájlbeolvasás

            Toast.makeText(
                this, getString(
                    R.string.file_read_success
                ),
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            // Hiba esetén most is jelzünk a felhasználónak
            // We will still notify the user in case of an error
            Toast.makeText(
                this, getString(
                    R.string.open_file_read_error
                ),
                Toast.LENGTH_SHORT
            ).show()
            // Most pedig kiírjuk a hibát konzolba is
            // Now we print the error to the console as well
            Log.e(TAG, "Error reading file: $uri", e)
        }
    }
    // Létrehozunk egy fájlbaíró függvényt
    // Let's create our fileWriter function
    /**
     * Beírja a CodeView tartalmát egy adott Uri-n keresztül elérhető fájlba.
     * Writes the content of the CodeView to a file accessible via a given Uri.
     */
    private fun writeTextToFile(uri: android.net.Uri) {
        try {
            val currentJsonText = binding.codeView.text.toString()

            val questionList: List<QuestionAnswer> = try {
                val parser = com.google.gson.Gson()
                parser.fromJson(
                    currentJsonText,
                    object : com.google.gson.reflect.TypeToken<List<QuestionAnswer>>() {}.type
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this, getString(R.string.error_Json_Save_Format),
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, getString(R.string.writeTextJsonError), e)
                return
            }
            // használjunk JsonProcessorFactoryt
            // Use JsonProcessorFactory
            val saverMode = JsonProcessorFactory.createSaver(this)

            // Használjuk a JsonElmentőt a végleges,
            // formázott JSON string létrehozásához.
            // Use JsonSave to create the final,
            // formatted JSON string.
            val jsonStringToSave = saverMode.createJsonString(
                questionList
            )

            if (jsonStringToSave == null) {
                Toast.makeText(
                    this, getString(
                        R.string.save_file_error
                    ), Toast.LENGTH_LONG
                ).show()
                return
            }

            // Mentsük el a fájlt a uri helyére.
            // Save the file to the uri location.
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.writer().write(jsonStringToSave)
            }

            // Üzenünk a felhasználónak a sikeres mentésről
            // Notify the user of the successful save

            // fontos a sikeres mentés után is eltároljuk az uri-t
            // Important: We store the URI after a successful save
            this.currentFileUri = uri
            Toast.makeText(
                this,
                getString(
                    R.string.save_file_success
                ),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            //Hiba esetén jelzünk a felhasználonak egy üzenetet
            // We will still notify the user in case of an error
            Toast.makeText(
                this,
                getString(
                    R.string.save_file_error
                ),
                Toast.LENGTH_LONG
            ).show()
            // Most pedig kiírjuk a hibát konzolba is
            // Now we print the error to the console as well
            Log.e(TAG, "Error writing file: $uri", e)
        }
    }

    /**
     * A CodeView frissítése a sorszámozás javításához.
     * Updates the CodeView to fix the line numbering.
     */
    private fun refreshCodeView() {
        // Újrafuttatja a szintaxis kiemelőt a könyvtár saját API-jával,
        // ami a sorszámokat is frissíti.
        // Re-runs the syntax highlighter using the library's own API,
        // which also refreshes the line numbers.
        binding.codeView.reHighlightSyntax()
    }

    // A gombok eseménykezelőjének a függvénye
// The function of the button event handler
    private fun setupButtonListeners() {

        // Másolás vágolapra gomb eseménykezelője
        // Copy button event handler
        binding.copyButton.setOnClickListener {

            // A Clipboardmanager rendszerszintü szolgáltatás incializálása
            // Initialize the ClipboardManager service
            val clipboard = getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

            // Kérjük le a teljes (tartalmat) szöveget a Codeview-ből
            // Please download the full (content) text from Codeview
            val textToCopy = binding.codeView.text.toString()

            // Hozzuk létre a clipData tartalmat a teljes szöveggel
            // Create the clipDate with the all text
            val clipData = ClipData.newPlainText(
                getString(R.string.Clipdata_copied_code), textToCopy
            )

            // Helyezzük a ClipDatát a vágolapra
            // Place the ClipDate on the clipboard
            clipboard.setPrimaryClip(clipData)

            //Üzenet a felhasználónak || Message to the user
            Toast.makeText(
                this, getString(R.string.ClipBoard_text),
                Toast.LENGTH_SHORT
            ).show()
        }

        // A beilesztés vágolapról gomb eseménykezelője
        // Event handler for the paste from clipboard button
        binding.pasteButton.setOnClickListener {

            // A Clipboardmanager rendszerszintü szolgáltatás incializálása
            // Initialize the ClipboardManager service
            val clipboard = getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager

            // Ellenőrizük hogy van-e adat a vágólapon
            // Check if there is data on the clipboard
            if (clipboard.hasPrimaryClip()) {

                // Le kérjük az adatokat
                // Get the data
                val clipData = clipboard.primaryClip

                // ellenőrizük hogy az adat szöveg és van legalább egy eleme
                // Check if the data is text and has at least one element
                if (clipData != null && clipData.itemCount > 0 &&
                    clipData.description.hasMimeType(
                        ClipDescription.MIMETYPE_TEXT_PLAIN
                    )
                ) {

                    // Lekérjük az első elemet a szövegböl
                    // Get the first element from the text
                    /*
            Az Android operációs rendszer a felhasználó által kijelölt
             és másolt szöveget (legyen az egyetlen szó, két sor,
              vagy akár egy komplett 1000 soros kód) egyetlen l
              ogikai egységként kezeli, és ezt az egységet teszi
               be a vágólap ClipData objektumának első (0-s indexű) elemébe.
               *
               The Android operating system treats the text selected and copied
                by the user (whether it is a single word, two lines, or
                even a complete 1000 lines of code) as a single logical
                unit, and places this unit in the first (0-indexed)
                element of the ClipData object of the clipboard.
             */
                    val textToPaste = clipData.getItemAt(0).text.toString()

                    // Most beillesztjük a szöveget a codeViewbe
                    // Now we paste the text into codeView
                    binding.codeView.setText(textToPaste)

                    // A szintaxis és a sorszám frisítésse
                    // Refresh the syntax and line numbers
                    refreshCodeView()


                    // Üzenünk a felhasználónak
                    // Notify the user
                    Toast.makeText(
                        this, getString(R.string.ClipBoard_success_message),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Ha a vágólapon nem szöveg van (pl. kép).
                    // If the clipboard contains something other than text (e.g., an image).
                    Toast.makeText(
                        this, getString(R.string.paste_not_text_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Ha nincs adat a vágólapon
                // If there is no data on the clipboard
                Toast.makeText(
                    this, getString(R.string.Clipboard_paste_no_data_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // A visszavonás gomb eseménykezelője
        // The undo button event handler
        binding.undoButton.setOnClickListener {

            // ellenőrizük van-e mit visszavonni a verembe
            // Check if there is something to undo
            if (historyStack.isNotEmpty()) {
                // Kivesszük a legutóbbi állapotot a veremből
                // (a .pop() metódus kiveszi és vissza is adja).
                // We take the last state from the stack
                // (the .pop() method takes it out and returns it).
                //val previousText = historyStack.pop()
                val previousText = historyStack.removeLast()

                // Beállítjuk a visszaállított szöveget a codeView-ba.
                // Set the restored text in the codeView.
                binding.codeView.setText(previousText)

                // A szintaxis és a sorszám frisítésse
                // Refresh the syntax and line numbers
                refreshCodeView()

                // A visszavont szöveg a kurzor végére kerüljön ne az elejére
                // Set the cursor to the end of the restored text
                binding.codeView.setSelection(previousText.length)
            } else {
                // Ha a verem üres  jelezükk a felhasználónak.
                // If the stack is empty, we notify the user.
                Toast.makeText(
                    this,
                    getString(R.string.undoButton_stack_is_empty),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // A "Mindent töröl" gomb eseménykezelője
        // "Delete All" button event handler
        binding.allDeleteButton.setOnClickListener {

            // AlertDialog felúgró ablak a felhasználónak a tőrléss véglegesítésére
            // AlertDialog pop-up window for the user to finalize the deletion
            val deleteBuilder = android.app.AlertDialog.Builder(this)

            // Állítsuk be az üzenetett és a címet a string erőforrásokból
            // Set the message and title from the string resources
            deleteBuilder.setTitle(
                getString(
                    R.string.delete_all_dialog_title
                )
            )
            deleteBuilder.setMessage(
                getString(
                    R.string.delete_all_dialog_message
                )
            )

            // Adjuk hozzá a törlés pozitív gombját(megerősítés)
            // Add a positive delete button (confirmation)
            deleteBuilder.setPositiveButton(
                getString(
                    R.string.deleteBuilder_delete_button_text
                )
            ) { dialog, which ->
                // Nincs Clear() függvénye
                // There is no Clear() function.
                binding.codeView.setText("")

                // A szintaxis és a sorszám frisítésse
                // Refresh the syntax and line numbers
                refreshCodeView()


                // visszajelzés a sikeres tőrlésről
                // Feedback on successful deletion.
                Toast.makeText(
                    this, getString(
                        R.string.delete_success_message
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Adjuk hozzá a törlés negatív gombját(mégse)
            // Add a negative delete button (cancel)

            deleteBuilder.setNegativeButton(
                getString(
                    R.string.cancel_button_text
                )
            ) { dialog, which ->
                // nem csinálunk semmit csak bezárjuk
                // we don't do anything and just close
                dialog.dismiss()
            }
            // Itt jelenítjük meg a párbeszédablakot
            //
            val dialog = deleteBuilder.create()
            dialog.show()
        }
        // A Json formázása gomb eseménykezelője
        // The Json Format button event handler
        binding.beautifyButton.setOnClickListener {

            // lekérjük a jelenlegi szöveget
            // Get the current text
            val currentJsonText = binding.codeView.text.toString()

            // Most ellenőrizük van -e mit formázni
            // Now we check if there is something to format
            if (currentJsonText.isBlank()) {
                Toast.makeText(
                    this, getString(
                        R.string.beautify_empty_text
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            try {
                // Hozzunk létre egy PrettyPrint Gsonpéldányt
                // Create a PrettyPrint Gson instance
                val gson = GsonBuilder()
                    .setPrettyPrinting().create()

                // Értelmezük a stringet JsonElemként
                // Parse the string as a JsonElement
                val jsonElement = JsonParser.parseString(currentJsonText)

                // Alakítsuk avissza a JsonElementett
                // szép formázott stringgé
                // Format the JsonElement back to a pretty-formatted string
                val prettyJsonText = gson.toJson(jsonElement)

                // Állítsuk be az új formázott szöveget
                // Set the new pretty-formatted text
                binding.codeView.setText(prettyJsonText)
                // A szintaxis és a sorszám frisítésse
                // Refresh the syntax and line numbers
                refreshCodeView()

                // Visszajelzünk a felhasználónak a formázás sikeréről
                // Notify the user of the successful formatting
                Toast.makeText(
                    this, getString(
                        R.string.beautify_success_sucess
                    ),
                    Toast.LENGTH_SHORT
                ).show()

            } catch (_: JsonSyntaxException) {
                // Hiba esetén, ha a szöveg nem valid JSON
                // hiányzik egy zárojel bármi
                // If the text is not valid JSON
                // missing a closing bracket or braces or any
                Toast.makeText(
                    this, getString(
                        R.string.beautify_invalid_json
                    ),
                    Toast.LENGTH_LONG
                ).show()

            } catch (_: Exception) {

                // Bármilyen más hiba esetén "Elfogyott a memória" stb stb ritka
                // Any other errors like "Out of memory" etc etc are It is rare
                Toast.makeText(
                    this, getString(
                        R.string.json_any_error_message
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // A json Struktúra ellenőrzése gomb eseménykezelője
        // The Json Structure check button event handler
        binding.validateButton.setOnClickListener {

            // Visszaadjuk a jelenlegi szöveget
            // Return the current text
            val jsonToValidate = binding.codeView.text.toString()

            // ha a szöveg üres
            // if the text is empty
            if (jsonToValidate.isBlank()) {
                // Írunk egy üzenettet a felhasználónak
                // We write a message to the user
                Toast.makeText(
                    this, getString(
                        R.string.validate_empty_text
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                // Visszalépünk az eseménykezelőbe(setOnClickListener-be)
                // We go back to the event handler (setOnClickListener)
                return@setOnClickListener
            }
            // Kezeljük a hibákat
            // Handle errors
            try {
                // Megprobáljuk értelmezni a szöveget
                // Try to parse the text
                JsonParser.parseString(jsonToValidate)

                // Üzenünk a felhasználónak hogy minden rendben van
                // Notify the user that everything is fine
                Toast.makeText(
                    this, getString(
                        R.string.validate_success_text
                    ),
                    Toast.LENGTH_SHORT
                ).show()

            } catch (_: JsonSyntaxException) {
                // Ha a parseString hibát dob, a JSON érvénytelen.
                // If parseString throws an error, the JSON is invalid.
                Toast.makeText(
                    this, getString(
                        R.string.validate_invalid_json
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
                // Bármilyen más hiba esetén "Elfogyott a memória" stb stb ritka
                // Any other errors like "Out of memory" etc etc are It is rare
                Toast.makeText(
                    this, getString(
                        R.string.json_any_error_message
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // A Json fájl megnyitása gomb eseménykezelője
        // The Json file open button event handler
        binding.openFileButton.setOnClickListener {

            // Elinditjuk a fájlelválasztó ablakot
            // Start the file picker launcher
            openFileLauncher.launch(arrayOf("application/json"))
        }

        // A Json fájl mentése gomb eseménykezelője
        // The Json file save button event handler
        binding.jsonFileSaveButton.setOnClickListener {

            // Mielött mentenénk ellenőrizük van-e tartalom
            // Before saving we check if there is something to save

            if (binding.codeView.text.toString().isBlank()) {

                // Ha nincs írunk egy üzenetett a felhasználónak
                // If not, we write a message to the user.
                Toast.makeText(
                    this, getString(
                        R.string.save_file_empty_error
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            // Ellenőrizük hogy, van e már mentett Uri-nk(Fájlunk)
            // Check if there is already a saved Uri (file)

            // Deklaráljuk a currentFileUri változót más változóban
            // Declare the currentFileUri variable in another variable
            val fileToSaveUri = currentFileUri

            if (fileToSaveUri != null) {
                val builder = android.app.AlertDialog.Builder(this)

                // Állítsuk be az üzenetett és a címet a string erőforrásokból
                // Set the message and title from the string resources
                builder.setTitle(getString(R.string.overwrite_dialog_title))
                builder.setMessage(getString(R.string.overwrite_dialog_message))

                // Adjuk hozzá a mentés pozitív gombját(megerősítés)
                // Add a positive save button (confirmation)
                builder.setPositiveButton(
                    getString(
                        R.string.overwrite_button_text
                    )
                )
                { dialog, which ->
                    writeTextToFile(fileToSaveUri)
                }
                // Adjuk hozzá a mentés negatív gombját(mégse)
                // Add a negative save button (cancel)
                builder.setNegativeButton(
                    getString(
                        R.string.cancel_button_text
                    )
                )
                { dialog, which ->
                    dialog.dismiss()
                }
                builder.create().show()
            } else {
                // Ha még nincs fájl akkor a mentés másként funkció
                // If there is no file yet, the save as function is used
                createFileLauncher.launch("document.json")
            }
        }

// A Json fájl mentés másként gomb eseménykezelője
// The Json file save as button event handler
        binding.jsonFileSaveAsButton.setOnClickListener {
            // Mielőtt mentenénk, ellenőrizzük, van-e mit menteni
            // Before saving we check if there is something to save
            if (binding.codeView.text.toString().isBlank()) {
                Toast.makeText(
                    this, getString(R.string.save_file_empty_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Ez is a "fájl létrehozása" ablakot indítja.
            createFileLauncher.launch("document.json")
        }

// A Vissza a főmenü gomb eseménykezelője
// The Back to main menu button event handler
        binding.backToTheMainMenuButton.setOnClickListener {
            // Létrehozunk egy szándékot (Intent), hogy elindítsuk a MainActivity-t.
            // We create an Intent to start MainActivity.
            val intent = Intent(this, MainActivity::class.java)

            // Elinditjuk a MainActivity-t.
            // We start MainActivity.
            startActivity(intent)

            // Bezárjuk a jelenlegi Activity-t (EditQuestionActivity).
            // Close the current Activity (EditQuestionActivity).
            finish()
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
}




