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

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Custom seekbar with floating label.
 */
public class SeekBarWithLabelHorizontal {

    public interface Apply {
        void apply(SeekBar seekbar, int progress);
    }

    public final SeekBar seekBar;
    public final TextView seekLbl;

    // ---------------------------------------------------------------------------------------------

    public SeekBarWithLabelHorizontal(View rootView, int seekBarRes, int seekBarLbl, Apply apply) {
        seekLbl = rootView.findViewById(seekBarLbl);
        seekBar = rootView.findViewById(seekBarRes);

        if (seekLbl.getParent() instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) seekLbl.getParent();
            viewGroup.setClipChildren(false);
            viewGroup.setClipToPadding(false);
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                float thumb_x = (float) progress * seekBar.getWidth() / seekBar.getMax();
                float thumb_y = 0;
                String text = String.valueOf(progress);
                Rect txtBounds = new Rect();
                seekLbl.setText(text);
                seekLbl.getPaint().getTextBounds(text, 0, text.length(), txtBounds);
                seekLbl.setX(seekBar.getLeft() + thumb_x - txtBounds.width() / 2f);
                seekLbl.setY(seekBar.getTop() + thumb_y - Math.max(seekLbl.getHeight(), txtBounds.height()));
                seekLbl.animate().cancel();
                seekLbl.setAlpha(1f);

                // Does not work
                // seekLbl.setLeft(Math.round(seekBar.getLeft() + thumb_x - txtBounds.width()/2f));
                // seekLbl.setTop(Math.round(seekBar.getTop() + thumb_y - txtBounds.height()));
                // seekLbl.invalidate();

                seekBar.setSecondaryProgress(progress);
                apply.apply(seekBar, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
}