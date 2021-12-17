package xyz.monkeytong.hongbao.dialogs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.PreferenceDialogFragmentCompat;

import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.preferences.SeekBarDialogPreference;

/**
 * Created on 2021/12/18
 * Author: bigwang
 * Description:
 */
public class SeekBarPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_TEXT = "SeekBarPreferenceDialogFragment.progress";

    private SeekBar seekBar;
    private TextView textView;
    private String hintText;

    public static SeekBarPreferenceDialogFragmentCompat newInstance(String key) {
        final SeekBarPreferenceDialogFragmentCompat
                fragment = new SeekBarPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    private SeekBarDialogPreference getSeekBarDialogPreference() {
        return (SeekBarDialogPreference) super.getPreference();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        hintText = getContext().getString(R.string.delay_open);

        int delay = getSeekBarDialogPreference().getValue();
        this.seekBar = view.findViewById(R.id.delay_seekBar);
        this.seekBar.setProgress(delay);


        this.textView = view.findViewById(R.id.pref_seekbar_textview);
        setHintText(0);

        this.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setHintText(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setHintText(int delay) {
        if (delay == 0) {
            this.textView.setText(getContext().getString(R.string.delay_instantly) + hintText);
        } else {
            this.textView.setText(getContext().getString(R.string.delay_delay) + delay + getContext().getString(R.string.delay_sec) + getContext().getString(R.string.delay_then) + hintText);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            getSeekBarDialogPreference().setValue(seekBar.getProgress());
        }
    }
}
