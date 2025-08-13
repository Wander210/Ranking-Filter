package com.gdd.rankingfilter.view.screen.setting

import android.content.Intent
import android.widget.Toast
import com.gdd.rankingfilter.R
import com.gdd.rankingfilter.base.BaseFragment
import com.gdd.rankingfilter.databinding.FragmentSettingBinding
import androidx.core.net.toUri

class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {

    override fun initData() {
    }

    override fun setUpView() {
    }

    override fun setUpListener() {
        binding.ivBack.setOnClickListener { navigateBack() }

        binding.clLanguage.setOnClickListener { navigateTo(R.id.action_settingFragment_to_languageFragment) }
        binding.clFeedback.setOnClickListener { handleFeedbackClick() }
        binding.clShare.setOnClickListener { shareApp() }
    }

    private fun handleFeedbackClick() {
        try {
            val email = "support@gmail.com"
            val subject = getString(R.string.app_feedback)
            val body = getString(R.string.dear_support_team)

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:$email?subject=${android.net.Uri.encode(subject)}&body=${
                    android.net.Uri.encode(body)
                }".toUri()
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {

                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }

                if (fallbackIntent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(Intent.createChooser(fallbackIntent,
                        getString(R.string.send_email)))
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.no_email_app_found), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(),
                getString(R.string.error_opening_email, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.check_out_this_awesome_app))
                putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${requireContext().packageName}")
            }
            startActivity(Intent.createChooser(shareIntent,getString(R.string.share_app)))
        } catch (_: Exception) {
        }
    }
}