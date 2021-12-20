package xyz.monkeytong.hongbao.dialogs;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.preferences.SeekBarDialogPreference;

/**
 * Created on 2021/12/18
 * Author: bigwang
 * Description:
 */
public class SeekBarPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_PROGRESS = "SeekBarPreferenceDialogFragment.progress";

    private SeekBar seekBar;
    private TextView textView;
    private int progress;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            progress = getSeekBarDialogPreference().getValue();
        } else {
            progress = savedInstanceState.getInt(SAVE_STATE_PROGRESS);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_PROGRESS, progress);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        this.seekBar = view.findViewById(R.id.delay_seekBar);
        this.textView = view.findViewById(R.id.pref_seekbar_textview);

        this.seekBar.setProgress(progress);
        setHintText(progress);

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
            this.textView.setText(R.string.delay_instantly);
        } else {
            this.textView.setText(requireContext().getString(R.string.delay_delay, delay));
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int progress = seekBar.getProgress();
            final SeekBarDialogPreference preference = getSeekBarDialogPreference();
            if (preference.callChangeListener(preference)) {
                preference.setValue(progress);
            }
        }
    }
}
