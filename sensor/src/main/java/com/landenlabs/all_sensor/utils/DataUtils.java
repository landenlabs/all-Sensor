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

import android.text.SpannableStringBuilder;

import java.util.Collection;

public class DataUtils {
    public static CharSequence join(CharSequence delimiter, Object[] tokens) {
        return join(delimiter, tokens, 0);
    }

    public static CharSequence join(CharSequence delimiter, Object[] tokens, int startAt) {
        // StringBuilder sb = new StringBuilder();
        SpannableStringBuilder sb = new SpannableStringBuilder();

        for (int idx = startAt; idx < tokens.length; idx++) {
            Object token = tokens[idx];
            if (sb.length() != 0) {
                sb.append(delimiter);
            }
            if (token instanceof Object[])
                sb.append(join(delimiter, (Object[]) token));
            else if (token instanceof CharSequence) {
                sb.append((CharSequence) token);
            } else {
                sb.append(token.toString());
            }
        }

        return sb;
    }

    @SuppressWarnings("unchecked")
    public static <TT> ArrayListEx<TT> asList(Object... elements) {
        int cnt = count(elements);
        ArrayListEx<TT> list = new ArrayListEx<>(cnt);
        for (Object element : elements) {
            if (element instanceof Object[]) { // if (element.getClass().isArray()) {
                addAll(list, (TT[]) element);
            } else {
                list.add((TT) element);
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    static <T> int count(T[] elements) {
        int cnt = 0;
        for (T element : elements) {
            if (element instanceof Object[])
                cnt += count((T[]) element);
            else
                cnt++;
        }
        return cnt;
    }

    @SuppressWarnings("unchecked")
    static <T> void addAll(Collection<T> list, T[] elements) {
        for (T element : elements) {
            if (element.getClass().isArray())
                addAll(list, (T[]) element);
            else
                list.add(element);
        }
    }
}
