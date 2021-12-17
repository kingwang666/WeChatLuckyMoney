package xyz.monkeytong.hongbao.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import xyz.monkeytong.hongbao.dialogs.SeekBarPreferenceDialogFragmentCompat;

/**
 * Created on 2021/12/18
 * Author: bigwang
 * Description:
 */
public class BaseDialogPreference extends DialogPreference implements PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    private static final String DIALOG_FRAGMENT_TAG =
            "androidx.preference.PreferenceFragment.DIALOG";

    public BaseDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BaseDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BaseDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseDialogPreference(Context context) {
        super(context);
    }

    @Override
    public boolean onPreferenceDisplayDialog(@NonNull PreferenceFragmentCompat caller, Preference pref) {

        // check if dialog is already showing
        if (caller.getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return true;
        }

        final DialogFragment f;
        if (this instanceof SeekBarDialogPreference) {
            f = SeekBarPreferenceDialogFragmentCompat.newInstance(getKey());
        } else {
            return false;
        }
        f.setTargetFragment(caller, 0);
        f.show(caller.getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
        return true;
    }
}
