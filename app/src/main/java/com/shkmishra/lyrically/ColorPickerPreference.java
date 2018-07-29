package com.shkmishra.lyrically;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.pavelsikun.vintagechroma.ChromaDialog;
import com.pavelsikun.vintagechroma.IndicatorMode;
import com.pavelsikun.vintagechroma.OnColorSelectedListener;
import com.pavelsikun.vintagechroma.colormode.ColorMode;

public class ColorPickerPreference extends Preference {

    private String value = null;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.color_pref_layout);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        ImageView colorPreview = (ImageView) view.findViewById(R.id.color_preview);
        ((GradientDrawable) colorPreview.getBackground()).setColor(Color.parseColor(value));
    }

    @Override
    protected void onClick() {
        super.onClick();
        new ChromaDialog.Builder()
                .initialColor(Color.parseColor(value))
                .colorMode(ColorMode.RGB)
                .indicatorMode(IndicatorMode.DECIMAL)
                .onColorSelected(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        setValue(String.format("#%06X", (0xFFFFFF & color)));
                    }
                })
                .create().show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "Color Picker");
    }

    public void setValue(String value) {
        if (callChangeListener(value)) {
            this.value = value;
            persistString(value);
            notifyChanged();
        }
    }

    @Override
    protected String onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedString(null) : (String) defaultValue);
    }
}
