package com.attibexx.old_learning_app

/**
 * Ez az objektum tartalmazza az alkalmazás SharedPreferences kulcsait.
 * Egy központi hely a beállítási konstansok tárolására.
 * This object contains the SharedPreferences keys for the application.
 * A central place to store setup constants.
 */
object AppSettingsKeys {
    // A KÖZÖS BEÁLLÍTÁSOK FÁJLJÁNAK A NEVE
    // NAME OF THE COMMON SETTINGS FILE
    const val PREFS_FILE_NAME = "prefs"

    // JSON feldolgozási mód kulcsa
    // JSON processing mode key
    const val JSON_LOADER_MODE = "json_loader_mode"

    // Az alkalmazás nyelvének kulcsa
    // This is the application's language key
    const val LEARNING_APP_LANGUAGE = "learning_app_language"

    // Animációk engedélyezésének kulcsa
    // Key to enable animations
    const val ANIMATIONS_ENABLED = "animations_enabled"

    // Hint (segítség) engedélyezésének kulcsa
    // Hint (help) enable key
    const val HINTS_ENABLED = "hints_enabled"

    // Nagyítás engedélyezésének kulcsa
    // Zoom enable key
    const val ZOOM_ENABLED = "zoom_enabled"
}
