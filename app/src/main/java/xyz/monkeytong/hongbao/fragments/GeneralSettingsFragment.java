package xyz.monkeytong.hongbao.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.activities.WebViewActivity;
import xyz.monkeytong.hongbao.utils.SettingUtils;
import xyz.monkeytong.hongbao.utils.UpdateTask;

/**
 * Created by Zhongyi on 2/4/16.
 */
public class GeneralSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.general_preferences);
        setPrefListeners();
    }

    private void setPrefListeners() {
        //add white
        Preference whitePref = findPreference("pref_add_white");
        whitePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingUtils.gotoWhiteListSetting(getActivity());
                return false;
            }
        });

        Preference batteryPref = findPreference("pref_battery");
        batteryPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingUtils.gotoBatteryOptimizationSettings(getActivity());
                return false;
            }
        });

        // Check for updates
        Preference updatePref = findPreference("pref_etc_check_update");
        updatePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                new UpdateTask(getActivity(), true).update();
                return false;
            }
        });

        // Open issue
        Preference issuePref = findPreference("pref_etc_issue");
        issuePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent webViewIntent = new Intent(getActivity(), WebViewActivity.class);
                webViewIntent.putExtra("title", "GitHub Issues");
                webViewIntent.putExtra("url", getString(R.string.url_github_issues));
                webViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webViewIntent);
                return false;
            }
        });
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof OnPreferenceDisplayDialogCallback && ((OnPreferenceDisplayDialogCallback) preference).onPreferenceDisplayDialog(this, preference)) {
            return;
        }
        super.onDisplayPreferenceDialog(preference);
    }
}
