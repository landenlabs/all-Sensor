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

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CardPageAdapter
        extends RecyclerView.Adapter<CardPageAdapter.CardVH>
        implements CardData {

    private final ArrayList<Card> cards;
    private final CardShared shared;
    public final View.OnClickListener onClickListener;

    public CardPageAdapter( @NonNull ArrayList<Card> cards, @NonNull CardShared shared, View.OnClickListener onClickListener) {
        this.cards = cards;
        this.shared = shared;
        this.onClickListener = onClickListener;
    }
    @NonNull
    @Override
    public CardVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return Card.createViewHolder(parent, viewType, this);
    }

    @Override
    public void onBindViewHolder(@NonNull CardVH holder, int position) {
        holder.fillView(cards.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return cards.get(position).cardType();
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    public CardShared getShared() {
        return shared;
    }

    @Override
    public ArrayList<Card> getCards() {
        return cards;
    }

    @Override
    public View.OnClickListener getClickListener() {
        return this.onClickListener;
    }

    // =============================================================================================
    public static abstract class CardVH extends RecyclerView.ViewHolder {
        public final int viewType;
        public CardVH(@NonNull View viewCard, int viewType) {
            super(viewCard);
            this.viewType = viewType;
        }

        abstract public void fillView(Card card);
    }
}
