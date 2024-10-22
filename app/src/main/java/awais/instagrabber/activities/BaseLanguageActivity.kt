package awais.instagrabber.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import awais.instagrabber.utils.LocaleUtils
import awais.instagrabber.utils.ThemeUtils

abstract class BaseLanguageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Update the locale configuration
        LocaleUtils.updateConfig(this)
        
        // Change the theme before calling super
        ThemeUtils.changeTheme(this)
        
        // Call the superclass method
        super.onCreate(savedInstanceState)
    }
}
