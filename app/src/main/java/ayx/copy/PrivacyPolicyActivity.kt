package ayx.copy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ayx.copy.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupCreditLink()
    }

    private fun setupCreditLink() {
        val prefix = getString(R.string.privacy_credit_prefix)   // "Written by "
        val name   = getString(R.string.privacy_credit_name)     // "RaX"
        val full   = "$prefix$name"

        val spannable = SpannableString(full)
        val start = prefix.length
        val end   = full.length

        val clickSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/unknownraxbot"))
                startActivity(intent)
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@PrivacyPolicyActivity, R.color.accent)
                ds.isUnderlineText = false
                ds.isFakeBoldText  = true
            }
        }

        spannable.setSpan(clickSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvCredit.text = spannable
        binding.tvCredit.movementMethod = LinkMovementMethod.getInstance()
        binding.tvCredit.highlightColor  = ContextCompat.getColor(this, R.color.accent_dim)

        // Apply base color for the non-clickable prefix portion
        binding.tvCredit.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary))
    }
}
