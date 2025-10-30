package com.attibexx.old_learning_app

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Log.isLoggable
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.attibexx.old_learning_app.databinding.ActivityEditQuestionBinding


class EditQuestionActivity : AppCompatActivity() {

    companion object {
        // Logoláshoz használt címke
        // Label used for logging
        private const val TAG = "EditQuestionActivity"
    }


    // binding példányosítása || Instance of binding
    private lateinit var binding: ActivityEditQuestionBinding

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

        }

        // A "Mindent töröl" gomb eseménykezelője
        // "Delete All" button event handler
        binding.allDeleteButton.setOnClickListener {

            // AlertDialog felúgró ablak a felhasználónak a tőrléss véglegesítésére
            // AlertDialog pop-up window for the user to finalize the deletion
            val deleteBuilder = android.app.AlertDialog.Builder(this)

            // Állítsuk be az üzenetett és a címet a string erőforrásokból
            // Set the message and title from the string resources
            deleteBuilder.setTitle(getString(
                R.string.delete_all_dialog_title))
            deleteBuilder.setMessage(getString(
                R.string.delete_all_dialog_message))

            // Adjuk hozzá a törlés pozitív gombját(megerősítés)
            // Add a positive delete button (confirmation)
            deleteBuilder.setPositiveButton(getString(
                R.string.deleteBuilder_delete_button_text)) {
                dialog,which ->
                // Nincs Clear() függvénye
                // There is no Clear() function.
                binding.codeView.setText("")

                // visszajelzés a sikeres tőrlésről
                // Feedback on successful deletion.
                Toast.makeText(this, getString(
                    R.string.delete_success_message),
                    Toast.LENGTH_SHORT).show()
            }
            // Adjuk hozzá a törlés negatív gombját(mégse)
            // Add a negative delete button (cancel)

            deleteBuilder.setNegativeButton(getString(
                R.string.deleteBuilder_cancel_button_text)) {
                dialog, which ->
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

        }
        // A json Struktúra ellenőrzése gomb eseménykezelője
        // The Json Structure check button event handler
        binding.validateButton.setOnClickListener {

        }
        // A Json fájl megnyitása gomb eseménykezelője
        // The Json file open button event handler
        binding.openFileButton.setOnClickListener {

        }
        // A Json fájl mentése gomb eseménykezelője
        // The Json file save button event handler
        binding.jsonFileSaveButton.setOnClickListener {

        }
        // A Json fájl mentés másként gomb eseménykezelője
        // The Json file save as button event handler
        binding.jsonFileSaveAsButton.setOnClickListener {

        }
        // A Vissza a főmenü gomb eseménykezelője
        // The Back to main menu button event handler
        binding.backToTheMainMenuButton.setOnClickListener {

        }




        //TODO
    }

    // Egy segédfüggvény, ami frissíti a CodeView-et
// a sorszámozási hiba javítására
// A helper function that updates CodeView
// to fix the sequence numbering error
    private fun updateCodeViewAdapter() {

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

