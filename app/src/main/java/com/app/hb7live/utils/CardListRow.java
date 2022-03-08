
package com.app.hb7live.utils;


import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ObjectAdapter;

import com.app.hb7live.models.CardRow;

/**
 * with or without a shadow.
 */
public class CardListRow extends ListRow {

  private CardRow mCardRow;

  public CardListRow(HeaderItem header, ObjectAdapter adapter, CardRow cardRow) {
    super(header, adapter);
    setCardRow(cardRow);
  }

  public CardRow getCardRow() {
    return mCardRow;
  }

  public void setCardRow(CardRow cardRow) {
    this.mCardRow = cardRow;
  }
}