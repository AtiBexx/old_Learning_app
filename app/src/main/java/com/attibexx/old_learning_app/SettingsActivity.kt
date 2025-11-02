package com.attibexx.old_learning_app


import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Log.isLoggable
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.attibexx.old_learning_app.databinding.SettingsBinding


class SettingsActivity : AppCompatActivity() {


    private lateinit var binding: SettingsBinding

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

        }

        // json folyamat választása gomb
        // Json processing Button
        binding.jsonProcessingButton.setOnClickListener {

        }

        // Információs gomb
        // Information Button
        binding.informationButton.setOnClickListener {

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

