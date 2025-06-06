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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Extend functionality of ArrayListEx
 */
@SuppressWarnings("ALL")
public class ArrayListEx<E> extends ArrayList<E> {

    @SuppressWarnings("unused")
    public ArrayListEx(int initialCapacity) {
        super(initialCapacity);
    }

    public ArrayListEx(Collection<? extends E> collection) {
        super(collection);
    }

    public ArrayListEx(E[] array) {
        super(array.length);
        addAll(array);
    }

    public static <E>  boolean isEmpty(@Nullable ArrayListEx<E> array) {
        return array == null || array.size() == 0;
    }

    @SafeVarargs
    public final void addAll(E... values) {
        ensureCapacity(size() + values.length);
        for (E value : values) {
            add(value);
        }
    }

    public ArrayListEx(E singleItem) {
        super(Arrays.asList(singleItem));
    }

    public ArrayListEx() {
        super();
    }

    @NonNull
    public static <E> E get(ArrayListEx<E> list, int idx, E defValue) {
        return (list != null && idx >= 0 && idx < list.size() && list.get(idx) != null)
                ? list.get(idx) : defValue;
    }

    public static <E> E first(ArrayListEx<E> list, E def) {
        return list == null ? def : list.first(def);
    }

    public static <E> E last(ArrayListEx<E> list, E def) {
        return (list == null) ? def : list.last(def);
    }


    // ---------------------------------------------------------------------------------------------
    // Static

    public static <E> int size(ArrayListEx<E> list, int def) {
        return (list == null) ? def : list.size();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public static <E> boolean equals(List<E> list1, List<E> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        return list1.equals(list2);
    }

    /**
     * Merge two lists avoid duplicates. List2 add after list1.
     */
    @Nullable
    public static <F extends ArrayListEx<E>, E> ArrayListEx<E> merge(@Nullable F list1, @Nullable F list2) {
        if (list1 == null) {
            return list2 == null ? null : new ArrayListEx<E>(list2);
        } else if (list2 == null) {
            return new ArrayListEx<E>(list1);
        }

        ArrayListEx<E> outList = new ArrayListEx<>(list1.size() + list2.size());
        outList.addAll(list1);
        for (E item : list2) {
            if (!outList.contains(item)) {
                outList.add(item);
            }
        }

        return outList;
    }

    public E get(int idx, E defValue) {
        return (idx >= 0 && idx < size()) ? get(idx) : defValue;
    }

    public E first(E def) {
        return isEmpty() ? def : get(0);
    }

    public E last(E def) {
        return isEmpty() ? def : get(size() - 1);
    }

    // --- Back port to < API 24 ---
    // public void sort(Comparator<? super E> c, Object ignore) {
    //    java.util.Collections.sort(this, c);
    // }
    synchronized
    public void sort(Comparator<? super E> c) {
        if (Build.VERSION.SDK_INT >= 24 /* Build.VERSION_CODES.N */) {
            super.sort(c);
        } else {
            java.util.Collections.sort(this, c);
        }
    }

    // Iterates backwards and removes items when filter is true.
    @Override
    synchronized
    public boolean removeIf(Predicate<? super E> filter) {
        boolean removed = false;
        for (int i=size()-1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            final E element = (E) get(i);
            if (filter.test(element)) {
                remove(i);
                removed = true;
            }
        }

        return removed;
    }
}
