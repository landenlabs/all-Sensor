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

package com.landenlabs.all_sensor.utils;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;

import androidx.annotation.NonNull;

import java9.util.function.Function;

public class UiUtils {

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <TT extends Checkable, RR> ArrayListEx<RR> findChecked(
            @NonNull ViewGroup viewGroup, Function<TT, RR> getValue) {
        ArrayListEx<RR> checkables = new ArrayListEx<>(viewGroup.getChildCount());
        for (int idx = 0; idx < viewGroup.getChildCount(); idx++) {
            View child = viewGroup.getChildAt(idx);
            if (child instanceof Checkable) {
                TT checkable = (TT) child;
                if (checkable.isChecked()) {
                    checkables.add(getValue.apply(checkable));
                }
            }
        }
        return checkables;
    }

    public static void setSelected(boolean selected, View... views) {
        for (View view : views) {
            if (view != null) view.setSelected(selected);
        }
    }

    public static void ifSetVisibility(int visibility, View... views) {
        for (View view : views) {
            if (view != null) {
                view.setVisibility(visibility);
            }
        }
    }

    public static void togleSelected(View view) {
        if (view != null) view.setSelected(!view.isSelected());
    }
}
