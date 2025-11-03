package com.attibexx.old_learning_app


import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Log.isLoggable
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.attibexx.old_learning_app.databinding.SettingsBinding
import androidx.core.content.edit
import com.attibexx.old_learning_app.json.JsonProcessorFactory


class SettingsActivity : AppCompatActivity() {


    private lateinit var binding: SettingsBinding

    // Közös beállítás
    // common setting
    private val prefs by lazy { getSharedPreferences(JsonProcessorFactory.PREFS_FILE_NAME, MODE_PRIVATE) }

    companion object {
        // Logoláshoz használt címke
        // Label used for logging
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Beállítjuk hogy ne lógjon bele a képernyő a gombokhoz és a fenti eszköztárba
        //We set the screen to not hang in the buttons and the toolbar above
        enableEdgeToEdge()

        //A képernyő fellépítése a Layout alapján
        //The screen fills the layout
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A gombok eseménykezelőinek meghívása
        // Call the button event handlers
        setupButtonListeners()
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

    /**
     *A Gombok eseménykezelője
     *The Button of Listeners
     */
    private fun setupButtonListeners() {

        // Téma választása gomb
        // Theme selection button
        binding.selectThemeButton.setOnClickListener {

            // Választható opciók listája az array.xml-ből
            // List of selectable options from the array.xml
            val themeOptions =
                resources.getStringArray(R.array.theme_options)

            // Létrehozuk az AlerDialog buildert
            // fontos androidx.appcombat.app.AlertDialog legyen ne a sima
            // We create the AlertDialog builder
            // It is important to use androidx.appcompat.app.AlertDialog
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle(R.string.select_theme_title)

            // A builder.setItems helyett a setSingleChoiceItems metódust használjuk,
            // ami rádiógombokat jelenít meg, és megjegyzi a kiválasztott elemet.
            // We use the setSingleChoiceItems method instead of the setItems method,
            // which displays radio buttons and marks the selected element.
            builder.setSingleChoiceItems(themeOptions, -1) { dialog, which ->
                // Ez a blokk azonnal lefut és megjegyzi ha a felhasználó beállítja
                // a kiválasztott témát.
                // This block runs immediately and remembers when the user sets
                // the selected theme.
                val selectedThemeMode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_NO // világos || light
                    1 -> AppCompatDelegate.MODE_NIGHT_YES// sötét || dark
                    2 -> AppCompatDelegate.MODE_NIGHT_YES     // Budgie téma || Budgie theme
                    3 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // Alapértelmezett || Default
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // Alapértelmezett || Default
                }
                // Itt alkalmazzuk a kiválasztott témát az egész alkalmazásra
                // Here we apply the selected theme to the entire application

                AppCompatDelegate.setDefaultNightMode(selectedThemeMode)

                // Bezárjuk az ablakot
                // Close the window
                dialog.dismiss()
            }
            // Mégse gomb
            // Add Cancel Button
            builder.setNegativeButton(R.string.cancel_button_text) { dialog, _ ->
                dialog.dismiss()
            }
            // Létrehozuk és megjelenítjül az ablakot
            // We create and display the window
            val dialog = builder.create()
            dialog.show()
        }

        // json folyamat választása gomb
        // Json processing Button
        binding.jsonProcessingButton.setOnClickListener {

            // A választható opciók listája
            // List of selectable options
            val jsonProcessorOptions =
                resources.getStringArray(R.array.json_processor_options)

            // A konstansok, amiket el fogunk menteni
            // The constants we will store
            val processorModes = listOf(
                JsonProcessorFactory.MODE_SERIALIZABLE,
                JsonProcessorFactory.MODE_GSON,
                JsonProcessorFactory.MODE_MANUAL_JSON,
                JsonProcessorFactory.MODE_CPP
            )
            // Olvassuk ki a JELENLEGI mentett módot (ami egy string!)
            // Read the PREVIOUSLY saved mode (which is a string!)
            val savedMode = prefs.getString(
                JsonProcessorFactory.JSON_LOADER_MODE,
                JsonProcessorFactory.MODE_SERIALIZABLE
            )
            // Keressük meg, hogy a mentett mód hanyadik a listában,
            // hogy a rádiógomb a helyes opción álljon.
            // Let's find out where the saved mode is in the list,
            // so that the radio button in the right option is selected.
            val savedChoiceIndex = processorModes.indexOf(savedMode).let {
                if (it == -1) 0 else it }

            // Az AlertDialog felépítése
            // Structure of AlertDialog
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle(R.string.select_json_processor)

            builder.setSingleChoiceItems(jsonProcessorOptions,
                savedChoiceIndex) { dialog, which ->

                // Az index alapján válasszuk ki az elmentendő string konstanstott
                // Based on the index, we select the string constant
                val modeToSave = processorModes[which]

                // Elmentjük az új választást (a stringet!) a SharedPreferences-be
                // Save the new choice (the string!) to the SharedPreferences
                prefs.edit {
                    putString(JsonProcessorFactory.JSON_LOADER_MODE, modeToSave)
                }
                // Visszajelzés a felhasználónak
                // Feedback for User
                val chosenOptionText = jsonProcessorOptions[which]
                Toast.makeText(this,
                    getString(R.string.choosenJsonProcess,
                        chosenOptionText),
                    Toast.LENGTH_SHORT
                ).show()

                // Dialógus bezárása
                // Dialog close
                dialog.dismiss()
            }
            // Mégse gomb
            // Cancel button
            builder.setNegativeButton(R.string.cancel_button_text, null)

            // Dialógus megjelenítése
            // Dialog display
            builder.create().show()
        }
        // Információs gomb
        // Information Button
        binding.informationButton.setOnClickListener {

        }
        // nyelválasztási gomb
        // Language selection button
        binding.languageSelectionButton.setOnClickListener {

        }

        //Egyéb beállítások
        // Other settings
        binding.otherSettingsButton.setOnClickListener {

        }
        // Vissza a főmenübe gomb
        // Back to main menu button
        binding.backToTheMainMenuButton.setOnClickListener {
            finish()
            // A finish() függvény
            // Bezárja az aktuális Activity-t és visszatér az előzőhöz a főmenübe
            //The finish() function
            // Closes the current Activity and returns to the main menu
        }
    }
}

