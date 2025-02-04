/*
 * Unpublished Work © 2020 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Dennis Lang
 * @see http://LanDenLabs.com/
 */

package com.landenlabs.all_sensor.ui.utils;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.landenlabs.all_sensor.R;

/**
 * Buttons with animated background.
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class BusyButton extends androidx.appcompat.widget.AppCompatButton {
    public BusyButton(Context context) {
        super(context);
    }

    public BusyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BusyButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        super.invalidateDrawable(drawable);
        setCustomEnable(getEnabled());
    }

    public boolean getEnabled() {
        boolean enabled = (isDuplicateParentStateEnabled() && getParent() instanceof View)
                ? ((View) getParent()).isEnabled()
                : super.isEnabled();
        return enabled;
    }


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCustomEnable(enabled);
    }

    boolean barrier = false;

    private void setCustomEnable(boolean enabled) {
        if (!barrier) {
            barrier = true;
            if (enabled && getForeground() != null) {
                setForeground(null);
            } else if (!enabled && getForeground() == null) {
                AnimatedVectorDrawable animVecDraw = (AnimatedVectorDrawable)
                        ResourcesCompat.getDrawable(getResources(), R.drawable.vec_anim_pulse, getContext().getTheme());
                if (animVecDraw != null) {
                    animVecDraw.start();
                    setForeground(animVecDraw);
                }
            }
            barrier = false;
        }
    }
}
