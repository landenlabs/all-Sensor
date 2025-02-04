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

package com.landenlabs.all_sensor.ui.cards;

import android.content.Context;
import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.utils.SpanUtil;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public abstract class Card {

    public enum CardTypes { TmpHum, Pressure, WiFi, Battery, GPS, Elev, Speed, Light, Sound, Cpu }

    public abstract int cardType();

    public abstract  CardPageAdapter.CardVH createViewHolder(@NonNull View viewCard, int viewType, @NonNull CardShared shared);

    /*
    public static View inflate(@NonNull ViewGroup parent, int viewType, ArrayList<Card>cards) {
        View viewCard = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_tmp_hum, parent, false);
        return viewCard;
    }
     */

    @Nullable
    static Card findCard(int cardType, @NonNull ArrayList<Card> cards ) {
        for (Card card : cards) {
            if (card.cardType() == cardType) {
                return card;
            }
        }
        assert true;        // Should never get here
        return null;
    }

    public static CardPageAdapter.CardVH createViewHolder(@NonNull ViewGroup parent, int cardType, @NonNull CardData cardData) {
        Card card = findCard(cardType, cardData.getCards());
        assert card != null;
        View viewCard = card.inflate(parent, cardType);
        viewCard.setOnClickListener(cardData.getClickListener());
        return card.createViewHolder(viewCard, cardType, cardData.getShared());
    }

    public abstract View inflate(@NonNull ViewGroup parent, int cardType);

    protected static SpannableString fmtTimeAgo(@NonNull Context context, @NonNull DateTime lastTime) {
        SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d E hh:mm a");
        int deltaMin = Minutes.minutesBetween( lastTime, DateTime.now()).getMinutes();
        String strDelta = "";
        if (deltaMin == 0) {
        } else if (deltaMin < 60) {
            strDelta = " " + context.getResources().getQuantityString(R.plurals.start_minAgo, deltaMin, deltaMin);
        } else {
            float deltaHr = (float)deltaMin / 60.0f;
            strDelta = " " + context.getString(R.string.start_hrAgo, deltaHr);
        }
        String tmStr = dateFmt.format(lastTime.getMillis()) + strDelta;
        int small = 2 + 1 + strDelta.length();
        SpannableString ssTm = SpanUtil.SString(tmStr, SpanUtil.SS_SMALLER_80, tmStr.length() - small, tmStr.length());
        return ssTm;
    }
}
