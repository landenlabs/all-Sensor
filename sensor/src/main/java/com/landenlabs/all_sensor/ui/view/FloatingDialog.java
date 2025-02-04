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

package com.landenlabs.all_sensor.ui.view;

import static com.landenlabs.all_sensor.Units.INVALID_RES_ID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Space;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * Custom floating dialog used for menus and dialogs.
 */
public class FloatingDialog
        implements Runnable, View.OnClickListener, View.OnTouchListener {

    private final static int FLING_DST = 300;
    @NonNull
    protected final View holder;
    @NonNull
    protected final View background;
    @NonNull
    protected final ViewGroup parent;
    private final int closeBtnRes;
    protected Factions faction = Factions.VertFromBot;
    private float startX = 0;
    private float startY = 0;
    private View dragger;
    private boolean isDragging = false;
    private Action action = Action.NONE;

    // ---------------------------------------------------------------------------------------------
    public FloatingDialog(
            @NonNull ViewGroup parent,
            @LayoutRes int dialogRes,
            @LayoutRes int backgroundRes,
            @IdRes int closeBtnRes) {
        Context context = parent.getContext();
        holder = LayoutInflater.from(context).inflate(dialogRes, parent, false);
        background = (backgroundRes != INVALID_RES_ID)
                ? LayoutInflater.from(context).inflate(backgroundRes, parent, false)
                : new Space(context);
        this.closeBtnRes = closeBtnRes;
        this.parent = parent;
        holder.findViewById(closeBtnRes).setOnClickListener(this);
        holder.setOnTouchListener(this);
    }

    @SuppressWarnings("unchecked")
    public <E extends View> E viewById(int id) {
        return holder.findViewById(id);
    }

    public FloatingDialog setAction(Factions action) {
        this.faction = action;
        return this;
    }

    Context getContext() {
        return holder.getContext();
    }

    View getRootView() {
        return holder.getRootView();
    }

    String getString(@StringRes int strRes) {
        return holder.getResources().getString(strRes);
    }

    @Override
    public void run() {
        switch (action) {
            case OPEN:
                faction.open(parent, holder, background);
                break;
            case CLOSE:
                holder.animate().translationY(-holder.getHeight())
                        .withEndAction(() -> {
                            if (holder.getParent() != null) {
                                parent.removeView(holder);
                            }
                            holder.setVisibility(View.INVISIBLE);
                        })
                        // .translationZ(0)
                        .setDuration(faction.closeMilli).start();
                break;
        }
    }

    public FloatingDialog open() {
        if (background.getParent() == null) {
            if (holder.getParent() != null) {
                parent.removeView(holder);
            }
            holder.setVisibility(View.INVISIBLE);
            parent.addView(holder);
            background.setVisibility(View.INVISIBLE);
            parent.addView(background);

            action = Action.OPEN;
            holder.post(this);  // wait for dialog to layout.
        }
        return this;
    }

    public FloatingDialog startAnim() {
        startAnim(holder);
        return this;
    }

    private void startAnim(View view) {
        if (view.getBackground() instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) view.getBackground()).start();
        } else if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            if (imageView.getDrawable() instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) imageView.getDrawable()).start();
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int idx = 0; idx < viewGroup.getChildCount(); idx++) {
                startAnim(viewGroup.getChildAt(idx));
            }
        }
    }

    public void close() {
        parent.removeView(background);
        action = Action.CLOSE;
        holder.post(this);
    }

    @Override
    @CallSuper
    public void onClick(View view) {
        int id = view.getId();
        if (id == closeBtnRes) {
            close();
        }
    }

    void setDragger(View dragger) {
        this.dragger = dragger;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (dragger != null) {
            Rect rect = new Rect();
            dragger.getGlobalVisibleRect(rect);
            boolean startDrag = rect.contains((int) event.getRawX(), (int) event.getRawY());
            if (startDrag || isDragging) {
                return dragView(holder, event);
            }
        }
        if (isFling(view, event)) {
            close();
        }
        return true;
    }

    private boolean dragView(View view, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startX = view.getX() - event.getRawX();
                startY = view.getY() - event.getRawY();
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getRawX() + startX;
                float y = event.getRawY() + startY;
                view.setX(x);
                view.setY(y);
                return true;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                break;
        }
        return true;
    }

    private boolean isFling(View view, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                startX = event.getX();
                startY = event.getY();
                break;
            case (MotionEvent.ACTION_UP):
                double dist = Math.sqrt(SQ(startX - event.getX()) + SQ(startY - event.getY()));
                // ALog.d.tagMsg(this, "dist=" + dist);
                return (dist > FLING_DST);
        }
        return false;
    }

    private float SQ(float v1) {
        return v1 * v1;
    }

    // =============================================================================================
    private enum Action {NONE, OPEN, CLOSE}

    @SuppressWarnings("SameParameterValue")
    public enum Factions {
        VertFromBot(Factions.DEF_OPEN_MILLI, Factions.DEF_FADE_MILLI, Gravity.BOTTOM) {
            @Override
            Factions open(@NonNull View parent, @NonNull View dialog, @NonNull View background) {
                super.open(parent, dialog, background);
                int transY = (parent.getHeight() - dialog.getHeight()) / 2;
                int holderX = (parent.getWidth() - dialog.getWidth()) / 2;
                int startY = parent.getHeight();
                int startElev = 10;
                int dialogH = dialog.getHeight();

                dialog.setTop(0);
                dialog.setTranslationY(startY);
                dialog.setElevation(1);
                dialog.setTranslationZ(startElev);
                dialog.setX(holderX);   // Center horizontally
                dialog.setVisibility(View.VISIBLE);

                background.setTop(dialogH);
                background.setBottom(transY + background.getTop());
                background.setTranslationY(startY);
                background.setAlpha(1);
                background.setElevation(0);
                background.setTranslationZ(startElev);
                background.setLeft(holderX);
                background.setRight(holderX + dialog.getWidth());
                background.setVisibility(View.VISIBLE);


                dialog.animate().translationY(transY)
                        // .translationZ(10)
                        .setDuration(openMilli)
                        .start();

                background.animate().translationY(transY)
                        // .translationZ(2)
                        .setDuration(openMilli)
                        .start();
                return this;
            }
        },
        HorzFromEnd(Factions.DEF_OPEN_MILLI, Factions.DEF_FADE_MILLI, Gravity.END) {
            @Override
            Factions open(@NonNull View parent, @NonNull View dialog, @NonNull View background) {
                super.open(parent, dialog, background);
                int transX = (parent.getWidth() - dialog.getWidth()) / 2;
                int holderY = (parent.getHeight() - dialog.getHeight()) / 2;
                int startX = parent.getWidth();
                int startElev = 10;
                int dialogW = dialog.getWidth();

                dialog.setLeft(0);
                dialog.setTranslationX(startX);
                dialog.setElevation(0);
                dialog.setTranslationZ(startElev);
                dialog.setY(holderY);   // Center vertically
                dialog.setVisibility(View.VISIBLE);

                background.setLeft(dialogW);
                background.setRight(transX + background.getLeft());
                background.setTranslationX(startX);
                background.setAlpha(1);
                background.setElevation(0);
                background.setTranslationZ(startElev);
                background.setTop(holderY);
                background.setBottom(holderY + dialog.getHeight());
                background.setVisibility(View.VISIBLE);

                dialog.animate().translationX(transX)
                        // .translationZ(10)
                        .setDuration(openMilli).start();

                background.animate().translationX(transX)
                        // .translationZ(2)
                        .setDuration(openMilli)
                        .start();
                return this;
            }
        };

        static final int DEF_OPEN_MILLI = 1500;
        static final int DEF_FADE_MILLI = 1000;
        final int openMilli;
        final int closeMilli;
        final int fadeMilli;

        Factions(int openMilli, int fadeMilli, int startEdge) {
            this.openMilli = openMilli;
            this.closeMilli = openMilli / 2;
            this.fadeMilli = fadeMilli;
        }

        Factions open(@NonNull View parent, @NonNull View dialog, @NonNull View background) {
            background.setAlpha(1.0f);
            // Cannot reuse same animate used in open translation
            background.animate().withEndAction(() -> {
                //  .setStartDelay(openMilli)
                background.animate().alpha(0)
                        .setDuration(fadeMilli)
                        .start();
            });
            return this;
        }
    }

}
