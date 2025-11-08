package com.attibexx.old_learning_app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Ez az osztály az alkalmazás belépési pontja, még a MainActivity előtt lefut.
 * Feladata, hogy az alkalmazás indulásakor beállítsa a mentett nyelvet.
 * This class is the entry point of the application, it runs before MainActivity.
 * Its task is to set the saved language when the application is started.
 */
class LearningApplicationLanguage : Application() {

    override fun onCreate() {
        super.onCreate()

        // Elővesszük a közös beállításainkat.
        // A kontextus itt az 'this', mivel az Application osztály maga is egy Context.
        // We take out our common settings.
        // The context here is 'this', because the Application class itself is a Context.
        val prefs = getSharedPreferences(
            AppSettingsKeys.PREFS_FILE_NAME, MODE_PRIVATE)

        // Kiolvassuk a mentett nyelvi kódot (pl. "hu", "en").
        // Ha nincs mentett kód, a 'languageCode' null lesz.
        // Read the saved language code (e.g. "hu", "en").
        // If there is no saved code, 'languageCode' will be null.
        val languageCode = prefs.getString(
            AppSettingsKeys.LEARNING_APP_LANGUAGE, null)

        // Csak akkor állítunk be nyelvet, ha van mentett érték.
        // Ha nincs (azaz az app először indul),
        // akkor a rendszer alapértelmezett nyelvét fogja használni.
        // We only set the language if there is a saved value.
        // If not (i.e. the app is launched for the first time),
        // then it will use the system default language.
        if (languageCode != null) {
            // Létrehozzuk a Locale objektumot a modern, nem elavult módszerrel.
            // We create the Locale object using the modern, non-deprecated method.
            val newLocale = Locale.forLanguageTag(languageCode)

            // Beállítjuk a nyelvet az egész alkalmazásra.
            // We set the language for the entire application.
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.create(newLocale)
            )
        }
    }
}
