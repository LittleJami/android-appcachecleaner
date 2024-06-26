package com.panruyiapp.accessibility.appcachecleaner.ui.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.panruyiapp.accessibility.appcachecleaner.R
import com.panruyiapp.accessibility.appcachecleaner.config.SharedPreferencesManager
import com.panruyiapp.accessibility.appcachecleaner.const.Constant
import com.panruyiapp.accessibility.appcachecleaner.ui.activity.AppCacheCleanerActivity
import com.panruyiapp.accessibility.appcachecleaner.ui.dialog.CustomListDialogBuilder
import com.panruyiapp.accessibility.appcachecleaner.util.LocaleHelper
import com.panruyiapp.accessibility.appcachecleaner.util.toFormattedString
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.util.unit.DataSize
import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        val context = requireContext()
        lifecycleScope.launch {
            initialize(context)
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
    }

    private suspend fun initialize(context: Context) {
        val locale = LocaleHelper.getCurrentLocale(context)

        initializeUiNightMode(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_ui_night_mode))
        )

        initializeSettingsMaxWaitTimeout(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_settings_delay_for_next_app_timeout)),
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_settings_max_wait_app_timeout)),
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_settings_max_wait_clear_cache_btn_timeout)),
            context,
            { SharedPreferencesManager.Settings.getDelayForNextAppTimeout(context) },
            { timeout -> SharedPreferencesManager.Settings.setDelayForNextAppTimeout(context, timeout) },
            { SharedPreferencesManager.Settings.getMaxWaitAppTimeout(context) },
            { timeout -> SharedPreferencesManager.Settings.setMaxWaitAppTimeout(context, timeout) },
            { SharedPreferencesManager.Settings.getMaxWaitClearCacheButtonTimeout(context) },
            { timeout -> SharedPreferencesManager.Settings.setMaxWaitClearCacheButtonTimeout(context, timeout) }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initializeFilterMinCacheSize(
                preferenceManager.findPreference(
                    context.getString(R.string.prefs_key_filter_min_cache_size)),
                context,
                { SharedPreferencesManager.Filter.getMinCacheSize(context) },
                { str ->
                    try {
                        val dataSize = DataSize.parse(str)
                        val minCacheSize = dataSize.toBytes()
                        if (minCacheSize > 0L)
                            SharedPreferencesManager.Filter.saveMinCacheSize(context,
                                // set minimal 1KB
                                if (minCacheSize > 1024L) minCacheSize else 1024L)
                        else
                            SharedPreferencesManager.Filter.removeMinCacheSize(context)
                    } catch (e: Exception) {
                        showToast(R.string.prefs_error_convert_filter_min_cache_size)
                    }
                }
            )
        }

        initializeExtraSearchText(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_clear_cache)),
            context, locale,
            context.getText(R.string.clear_cache_btn_text),
            { SharedPreferencesManager.ExtraSearchText.getClearCache(context, locale) },
            { value ->
                if (value.trim().isEmpty())
                    SharedPreferencesManager.ExtraSearchText.removeClearCache(context, locale)
                else
                    SharedPreferencesManager.ExtraSearchText.saveClearCache(context, locale, value)
            }
        )

        initializeExtraSearchText(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_storage)),
            context, locale,
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    context.getText(R.string.storage_settings_for_app)
                else -> context.getText(R.string.storage_label)
            },
            { SharedPreferencesManager.ExtraSearchText.getStorage(context, locale) },
            { value ->
                if (value.trim().isEmpty())
                    SharedPreferencesManager.ExtraSearchText.removeStorage(context, locale)
                else
                    SharedPreferencesManager.ExtraSearchText.saveStorage(context, locale, value)
            }
        )

        initializeCustomList(
            context,
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_custom_list_add)),
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_custom_list_edit)),
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_custom_list_remove)),
        )

        initializeListOfIgnoredApps(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_filter_list_of_ignored_apps))
        )
    }

    private fun initializeUiNightMode(pref: Preference?) {
        pref?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val context = requireContext()
                lifecycleScope.launch {
                    val nightMode =
                        if (SharedPreferencesManager.UI.getNightMode(context))
                            AppCompatDelegate.MODE_NIGHT_YES
                        else
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    AppCompatDelegate.setDefaultNightMode(nightMode)
                }
                true
            }
        }
    }

    private suspend fun initializeSettingsMaxWaitTimeout(
        prefDelayForNextAppTimeout: SeekBarPreference?,
        prefMaxWaitAppTimeout: SeekBarPreference?,
        prefMaxWaitClearCacheButtonTimeout: SeekBarPreference?,
        context: Context,
        getDelayForNextAppTimeout: suspend () -> Int,
        setDelayForNextAppTimeout: suspend (Int) -> Unit,
        getMaxWaitAppTimeout: suspend () -> Int,
        setMaxWaitAppTimeout: suspend (Int) -> Unit,
        getMaxWaitClearCacheButtonTimeout: suspend () -> Int,
        setMaxWaitClearCacheButtonTimeout: suspend (Int) -> Unit)
    {
        prefDelayForNextAppTimeout?.apply {
            min = Constant.Settings.CacheClean.MIN_DELAY_FOR_NEXT_APP_MS / 1000
            max = Constant.Settings.CacheClean.MAX_DELAY_FOR_NEXT_APP_MS / 1000
            setDefaultValue(Constant.Settings.CacheClean.DEFAULT_DELAY_FOR_NEXT_APP_MS / 1000)
            value = getDelayForNextAppTimeout()
            if (value < Constant.Settings.CacheClean.MIN_DELAY_FOR_NEXT_APP_MS / 1000)
                value = Constant.Settings.CacheClean.DEFAULT_DELAY_FOR_NEXT_APP_MS / 1000
            showSeekBarValue = true
            title = context.getString(R.string.prefs_title_delay_for_next_app_timeout, value)
            setOnPreferenceChangeListener { _, newValue ->
                title = context.getString(R.string.prefs_title_delay_for_next_app_timeout, newValue as Int)
                lifecycleScope.launch {
                    setDelayForNextAppTimeout(newValue)
                }
                true
            }
        }

        prefMaxWaitAppTimeout?.apply {
            min = Constant.Settings.CacheClean.MIN_WAIT_APP_PERFORM_CLICK_MS / 1000
            max = Constant.Settings.CacheClean.DEFAULT_WAIT_APP_PERFORM_CLICK_MS * 2 / 1000
            setDefaultValue(Constant.Settings.CacheClean.DEFAULT_WAIT_APP_PERFORM_CLICK_MS / 1000)
            value = getMaxWaitAppTimeout()
            if (value < Constant.Settings.CacheClean.MIN_WAIT_APP_PERFORM_CLICK_MS / 1000)
                value = Constant.Settings.CacheClean.DEFAULT_WAIT_APP_PERFORM_CLICK_MS / 1000
            showSeekBarValue = true
            title = context.getString(R.string.prefs_title_max_wait_app_timeout, value)
            setOnPreferenceChangeListener { _, newValue ->
                title = context.getString(R.string.prefs_title_max_wait_app_timeout, newValue as Int)
                lifecycleScope.launch {
                    setMaxWaitAppTimeout(newValue)
                    // shift Clear Cache button timeout
                    if (newValue < getMaxWaitClearCacheButtonTimeout())
                        setMaxWaitClearCacheButtonTimeout(newValue - 1)
                    prefMaxWaitClearCacheButtonTimeout?.apply {
                        max = getMaxWaitAppTimeout() - 1
                        value = getMaxWaitClearCacheButtonTimeout()
                        title = context.getString(R.string.prefs_title_max_wait_clear_cache_btn_timeout, value)
                    }
                }
                true
            }
        }

        prefMaxWaitClearCacheButtonTimeout?.apply {
            min = Constant.Settings.CacheClean.MIN_WAIT_CLEAR_CACHE_BUTTON_MS / 1000
            max = getMaxWaitAppTimeout() - 1
            setDefaultValue(Constant.Settings.CacheClean.DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS / 1000)
            value = getMaxWaitClearCacheButtonTimeout()
            if (value >= getMaxWaitAppTimeout() && value > 0)
                value = getMaxWaitAppTimeout() - 1
            showSeekBarValue = true
            title = context.getString(R.string.prefs_title_max_wait_clear_cache_btn_timeout, value)
            setOnPreferenceChangeListener { _, newValue ->
                title = context.getString(R.string.prefs_title_max_wait_clear_cache_btn_timeout, newValue as Int)
                lifecycleScope.launch {
                    setMaxWaitClearCacheButtonTimeout(newValue)
                }
                true
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun initializeFilterMinCacheSize(pref: EditTextPreference?,
                                          context: Context,
                                          getMinCacheSize: suspend () -> Long,
                                          onChangeMinCacheSize: suspend (String) -> Unit) {
        val showSummary = {
            val minCacheSize = runBlocking { getMinCacheSize() }
            if (minCacheSize > 0L) {
                val sizeStr = runBlocking { DataSize.ofBytes(minCacheSize).toFormattedString(context) }
                context.getString(R.string.prefs_summary_filter_min_cache_size, sizeStr)
            } else
                null
        }

        pref?.apply {
            dialogTitle = context.getString(R.string.dialog_filter_min_cache_size_message)
            summary = showSummary()
            setSummaryProvider {
                showSummary()
            }
            setOnBindEditTextListener { editText ->
                (activity as AppCacheCleanerActivity?)?.addOverlayJob(
                    suspendCallback = {
                        val value = getMinCacheSize()
                        if (value > 0L)
                            DataSize.ofBytes(value).toFormattedString(context)
                        else
                            null
                    },
                    postUiCallback = { text ->
                        editText.apply {
                            hint = "0 KB"
                            setText(text)
                        }
                    }
                )
            }
            setOnPreferenceChangeListener { _, newValue ->
                lifecycleScope.launch {
                    onChangeMinCacheSize(newValue as String)
                }
                true
            }
        }
    }

    private suspend fun initializeExtraSearchText(pref: EditTextPreference?,
                                          context: Context, locale: Locale,
                                          extraText: CharSequence,
                                          getExtraText: suspend () -> String?,
                                          onChangeExtraText: suspend (String) -> Unit) {
        pref?.apply {
            dialogTitle = context.getString(
                R.string.dialog_extra_search_text_message,
                locale.displayLanguage, locale.displayCountry,
                extraText)
            summary = getExtraText()
            setSummaryProvider {
                runBlocking { getExtraText() }
            }
            setOnBindEditTextListener { editText ->
                val value = runBlocking { getExtraText() }
                if (value?.isNotEmpty() == true) {
                    editText.setText(value)
                    editText.hint = null
                } else {
                    editText.text = null
                    editText.hint = extraText
                }
            }
            setOnPreferenceChangeListener { _, newValue ->
                lifecycleScope.launch {
                    onChangeExtraText(newValue as String)
                }
                true
            }
        }
    }

    private suspend fun initializeCustomList(context: Context,
                                     addPref: Preference?,
                                     editPref: Preference?,
                                     removePref: Preference?) {
        SharedPreferencesManager.PackageList.getNames(context).apply {
            editPref?.isVisible = isNotEmpty()
            removePref?.isVisible = isNotEmpty()
        }

        addPref?.apply {
            setOnPreferenceClickListener {
                // show dialog from Settings Fragment for better UX
                CustomListDialogBuilder.buildAddDialog(context) { name ->
                    name ?: return@buildAddDialog

                    (activity as AppCacheCleanerActivity?)?.addOverlayJob(
                        suspendCallback = {
                            // check if entered name already exists
                            val names = SharedPreferencesManager.PackageList.getNames(context)
                            if (names.contains(name))
                                showToast(R.string.toast_custom_list_add_already_exists)
                            else
                                (activity as AppCacheCleanerActivity?)?.showCustomListPackageFragment(name)
                        }
                    )
                }.show()
                true
            }
        }

        editPref?.apply {
            setOnPreferenceClickListener {
                (activity as AppCacheCleanerActivity?)?.addOverlayJob(
                    suspendCallback = {
                        SharedPreferencesManager.PackageList.getNames(context).sorted()
                    },
                    postUiCallback = { names ->
                        // show dialog from Settings Fragment for better UX
                        CustomListDialogBuilder.buildEditDialog(context, names) { name ->
                            name ?: return@buildEditDialog

                            (activity as AppCacheCleanerActivity?)?.addOverlayJob(
                                suspendCallback = {
                                    SharedPreferencesManager.PackageList.getNames(context).contains(name)
                                },
                                postUiCallback = { contains ->
                                    if (contains)
                                        (activity as AppCacheCleanerActivity?)?.showCustomListPackageFragment(name)
                                }
                            )
                        }.show()
                    }
                )
                true
            }
        }

        removePref?.apply {
            setOnPreferenceClickListener {
                (activity as AppCacheCleanerActivity?)?.addOverlayJob(
                    suspendCallback = {
                        SharedPreferencesManager.PackageList.getNames(context).sorted()
                    },
                    postUiCallback = { names ->
                        // show dialog from Settings Fragment for better UX
                        CustomListDialogBuilder.buildRemoveDialog(context, names) { name ->
                            name ?: return@buildRemoveDialog

                            (activity as AppCacheCleanerActivity?)?.addOverlayJob(
                                suspendCallback = {
                                    SharedPreferencesManager.PackageList.remove(context, name)
                                    SharedPreferencesManager.PackageList.getNames(context).apply {
                                        (activity as AppCacheCleanerActivity?)?.runOnUiThread {
                                            editPref?.isVisible = isNotEmpty()
                                            removePref?.isVisible = isNotEmpty()
                                        }
                                    }
                                    showToast(R.string.toast_custom_list_has_been_removed, name)
                                }
                            )
                        }.show()
                    }
                )
                true
            }
        }
    }

    private fun initializeListOfIgnoredApps(editListPref: Preference?) {
        editListPref?.apply {
            setOnPreferenceClickListener {
                (activity as AppCacheCleanerActivity?)?.showIgnoredListPackageFragment()
                true
            }
        }
    }

    private fun showToast(@StringRes resId: Int, vararg formatArgs: Any?) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), getString(resId, *formatArgs), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}