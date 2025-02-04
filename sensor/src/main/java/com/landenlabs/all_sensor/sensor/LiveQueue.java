/*
 * Copyright (c) 2022 Dennis Lang (LanDen Labs) landenlabs@gmail.com
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

package com.landenlabs.all_sensor.sensor;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Normal LiveData does not have a queue and only last value is processed.
 * If you post too quickly some can be ignored.
 *
 * This variant has a thread safe queue and are stored until needed.
 * When user gets called into their observer method, they must call next()
 * method to drain queue.
 *
 * <pre><code>
 *  LiveQueue<String myLiveData = new LiveQueue<>();
 *  myLiveData.observeForever(
 *      state -> { doSomething(state); myLiveData.next(); });  // Forever to get inActive lifeCycle updates
 *  myLiveData.observe(this,
 *      state -> { doSomething(state); myLiveData.next(); });
 * </code></pre>
 *
 */
public class LiveQueue<TT>  extends MutableLiveData<TT> {
    volatile boolean postIt = true;
    final ArrayBlockingQueue<TT> queue = new ArrayBlockingQueue<>(20);    // Set appropriate size

    //  Drain queue - if more data will recall observer.
    synchronized
    public void next() {

        if (queue.size() > 0) {
            try {
                super.postValue(queue.take());
            } catch (InterruptedException ignore) {
            }
        }
        postIt = (queue.size() == 0);
    }

    @Override
    public void postValue(@NonNull TT item) {
        try {
            queue.add(item);
        } catch (Exception ignore) {
            // Queue is overflowing, pre-allocate larger queue.
            System.err.println("LiveQueue overflowed");
        }
        if (postIt) {
            next();
        }
    }

}