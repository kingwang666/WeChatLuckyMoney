package xyz.monkeytong.hongbao.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;

import xyz.monkeytong.hongbao.R;

/**
 * Created by Zhongyi on 2/3/16.
 */
public class SeekBarDialogPreference extends BaseDialogPreference {

    private int mSeekBarValue;

    public SeekBarDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_seekbar);
        mSeekBarValue = getPersistedInt(0);
    }


    /**
     * Sets the current progress of the {@link SeekBar}.
     *
     * @param value The current progress of the {@link SeekBar}
     */
    public void setValue(int value) {
        final boolean wasBlocking = shouldDisableDependents();
        mSeekBarValue = value;
        persistInt(value);
        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
        notifyChanged();
    }

    /**
     * Gets the current progress of the {@link SeekBar}.
     *
     * @return The current progress of the {@link SeekBar}
     */
    public int getValue() {
        return mSeekBarValue;
    }

    @Override
    public boolean shouldDisableDependents() {
        return mSeekBarValue == 0 || super.shouldDisableDependents();
    }
}
