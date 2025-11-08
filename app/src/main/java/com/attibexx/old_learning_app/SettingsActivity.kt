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
import java.util.Locale
import com.attibexx.old_learning_app.AppSettingsKeys.JSON_LOADER_MODE
import com.attibexx.old_learning_app.AppSettingsKeys.PREFS_FILE_NAME


class SettingsActivity : AppCompatActivity() {


    private lateinit var binding: SettingsBinding

    // Közös beállítás
    // common setting
    private val prefs by lazy {
        getSharedPreferences(
            PREFS_FILE_NAME,
            MODE_PRIVATE
        )
    }

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
                JSON_LOADER_MODE,
                JsonProcessorFactory.MODE_SERIALIZABLE
            )
            // Keressük meg, hogy a mentett mód hanyadik a listában,
            // hogy a rádiógomb a helyes opción álljon.
            // Let's find out where the saved mode is in the list,
            // so that the radio button in the right option is selected.
            val savedChoiceIndex = processorModes.indexOf(savedMode).let {
                if (it == -1) 0 else it
            }

            // Az AlertDialog felépítése
            // Structure of AlertDialog
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle(R.string.select_json_processor)

            builder.setSingleChoiceItems(
                jsonProcessorOptions,
                savedChoiceIndex
            ) { dialog, which ->

                // Az index alapján válasszuk ki az elmentendő string konstanstott
                // Based on the index, we select the string constant
                val modeToSave = processorModes[which]

                // Elmentjük az új választást (a stringet!) a SharedPreferences-be
                // Save the new choice (the string!) to the SharedPreferences
                prefs.edit {
                    putString(JSON_LOADER_MODE, modeToSave)
                }
                // Visszajelzés a felhasználónak
                // Feedback for User
                val chosenOptionText = jsonProcessorOptions[which]
                Toast.makeText(
                    this,
                    getString(
                        R.string.choosenJsonProcess,
                        chosenOptionText
                    ),
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

            // Létrehozunk egy egyszerú dialógusAblakot
            // We create a one-time dialog box
            androidx.appcompat.app.AlertDialog.Builder(this)

                // cím beállítása string.xml-ből
                // title setting from string.xml
                .setTitle(R.string.information_title)

                // az üzenet beállítása string.xml-ből
                // the message is set from string.xml
                .setMessage(R.string.information_message)

                // Egy ok gomb, ami csak bezárja az ablakot
                // An ok button that just closes the window
                .setPositiveButton(android.R.string.ok, null)

                // Megjelenítjük az ablakot
                // We display the window
                .show()
        }
        // nyelválasztási gomb
        // Language selection button
        binding.languageSelectionButton.setOnClickListener {

            // A választható opciók listája és a hozzájuk tartozó kódok
            // List of selectable options and their codes
            val languageOptions = resources.getStringArray(
                R.array.language_options
            )
            val languageCodes = resources.getStringArray(
                R.array.language_codes
            )

            // Olvassuk ki a jelenleg mentett nyelvi kódot
            // Read the saved language code
            val savedLanguageCode = prefs.getString(AppSettingsKeys.LEARNING_APP_LANGUAGE, null)

            // Keressük meg, hogy a mentett kód hanyadik a listában,
            // hogy a rádiógomb a helyes opción álljon. Ha nincs mentve, -1.
            // Let's find out where the saved code is in the list,
            // so that the radio button in the right option is selected.
            // if not saved, -1
            val checkedItem = if (savedLanguageCode != null)
                languageCodes.indexOf(savedLanguageCode) else -1

            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle(R.string.select_language_title)

            // A checkedItem beállítja, melyik rádiógomb legyen kiválasztva
            // The checkedItem sets which radio button should be selected
            builder.setSingleChoiceItems(languageOptions, checkedItem)
            { dialog, which ->
                val selectedLanguageCode = languageCodes[which] // hu , eng etc
                val selectedLanguageName = languageOptions[which] //Magyar, Angol etc

                // Mentsük el a kiválasztott nyelvet a sharedPreferences-be
                // Save the selected language to the sharedPreferences
                prefs.edit {
                    putString(AppSettingsKeys.LEARNING_APP_LANGUAGE, selectedLanguageCode)
                }

                // Ez az új módszer nem elavult (deprecated)
                // This new method is not outdated (deprecated)
                val newLocale = Locale.forLanguageTag(selectedLanguageCode)
                AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.create(newLocale)
                )

                // Visszajelzés a felhasználónak a nyelv kiválasztásáról
                // Feedback for the user about the language selection
                Toast.makeText(
                    this,
                    getString(
                        R.string.select_language_title,
                        selectedLanguageName
                    ),
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
        //Egyéb beállítások gomb
        // Other settings button
        binding.otherSettingsButton.setOnClickListener {

            // Létrehozunk az egyedi layaoutot a dialogushoz
            // We create a unique layout for the dialog box
            val dialogView = layoutInflater.inflate(R.layout.dialog_other_settings, null)
            val switchAnimations =
                dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_animations)
            val switchHints =
                dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_hints)
            val switchZoom =
                dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_zoom)

            // Kiolvassuk a jelenleg mentett értékeket,
            // hogy a kapcsolók a helyes állásban legyenek
            val areAnimationsEnabled = prefs.getBoolean(
            AppSettingsKeys.ANIMATIONS_ENABLED, false)
            val areHintsEnabled = prefs.getBoolean(
                AppSettingsKeys.HINTS_ENABLED, false)
            val areZoomEnabled = prefs.getBoolean(
                AppSettingsKeys.ZOOM_ENABLED, false)

            // Beállítjuk a kapcsolók állapotát a mentett értékek alapján
            // Set the switch states according to the saved values
            switchAnimations.isChecked = areAnimationsEnabled
            switchHints.isChecked = areHintsEnabled
            switchZoom.isChecked = areZoomEnabled

            // Létrehozzuk az AlertDialog-ot
            // We create the AlertDialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.other_settings_title)
                .setView(dialogView) // Egyedi layaout beállítása || Set custom layout
                .setNegativeButton(R.string.cancel_button_text, null) // "Mégse" "Cancel" gomb button
                .setPositiveButton(R.string.button_save)
                { dialog, _ ->

                    // Mentjük az új értékeket a SharedPreferences-be
                    // Save the new values to the SharedPreferences
                    val newAnimationState = switchAnimations.isChecked
                    val newHintsState = switchHints.isChecked
                    val newZoomState = switchZoom.isChecked

                    prefs.edit {
                    putBoolean(AppSettingsKeys.ANIMATIONS_ENABLED, newAnimationState)
                    putBoolean(AppSettingsKeys.HINTS_ENABLED, newHintsState)
                    putBoolean(AppSettingsKeys.ZOOM_ENABLED, newZoomState)
                    }

                    // Visszajelzünk a felhasználónak
                    // We provide feedback to the user
                    Toast.makeText(
                        this,
                        R.string.settings_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                    // Létrehozzuk és megjelenítjük az AlertDialog-ot
                    // We create and display the AlertDialog
                .create()
                .show()
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

