/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.dirac;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.MainSwitchPreference;

/** MiSound's main switch uses the same compact, single-line scale as Dolby's ListItem. */
public class MiSoundMainSwitchPreference extends MainSwitchPreference {
    public MiSoundMainSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View frame = holder.itemView.requireViewById(
                com.android.settingslib.widget.mainswitch.R.id.frame);
        // A minimum, not a fixed height: translated labels and large fonts can still grow.
        frame.setMinimumHeight(dp(56));
        frame.setPaddingRelative(dp(16), 0, dp(16), 0);
        TextView title = holder.itemView.requireViewById(
                com.android.settingslib.widget.mainswitch.R.id.switch_text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
