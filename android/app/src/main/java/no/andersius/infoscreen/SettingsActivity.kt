package no.andersius.infoscreen

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var urlField: EditText
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        urlField = findViewById(R.id.url)
        errorText = findViewById(R.id.error)
        val save = findViewById<Button>(R.id.save)

        urlField.setText(FeedPrefs.url(this))
        urlField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                save()
                true
            } else {
                false
            }
        }

        save.setOnClickListener { save() }
        findViewById<Button>(R.id.reset).setOnClickListener {
            urlField.setText(FeedPrefs.DEFAULT_URL)
            errorText.text = ""
        }
        findViewById<Button>(R.id.cancel).setOnClickListener { finish() }

        // Start on Save so OK straight away accepts the current URL
        save.requestFocus()
    }

    private fun save() {
        val url = FeedPrefs.normalize(urlField.text.toString())
        if (url == null) {
            errorText.setText(R.string.settings_invalid_url)
            urlField.requestFocus()
            return
        }
        FeedPrefs.setUrl(this, url)
        finish()
    }
}
