package com.shkmishra.lyrically;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Akhil on 22-10-2016.
 */
public class SeekBarPreference2 extends Preference implements SeekBar.OnSeekBarChangeListener {
    boolean mTrackingTouch;
    private SeekBar mSeekBar;
    private TextView mSeekBarValue;
    private int mProgress;

    public SeekBarPreference2(Context context) {
        this(context, null, 0);
    }

    public SeekBarPreference2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreference2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_seekbar);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
        mSeekBarValue = (TextView) view.findViewById(R.id.seekbarValue);
        mSeekBar.setProgress(mProgress);
        mSeekBarValue.setText(mProgress + "");
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    // update the preference as the user moves the seekbar. Used for triggerOffset, triggerWidth and triggerHeight
    @Override
    public void onProgressChanged(
            SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && !mTrackingTouch) {
            syncProgress(seekBar);
        } else mSeekBarValue.setText(progress + "");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = false;
        if (seekBar.getProgress() != mProgress) {
            syncProgress(seekBar);
        }
    }


    private void setProgress(int progress, boolean notifyChanged) {
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(progress);
            mSeekBarValue.setText(progress + "");
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (progress != mProgress) {
            if (callChangeListener(progress)) {
                setProgress(progress, false);
            } else {
                seekBar.setProgress(mProgress);
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue);
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
        }

        if (value != mProgress) {
            mProgress = value;
            notifyChanged();
        }
    }


    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }
}