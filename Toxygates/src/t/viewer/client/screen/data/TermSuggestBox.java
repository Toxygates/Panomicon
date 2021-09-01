/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */
package t.viewer.client.screen.data;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import t.shared.common.Term;
import t.viewer.client.screen.data.TermSuggestOracle.TermSuggestion;

import java.util.ArrayList;
import java.util.List;

public class TermSuggestBox extends SuggestBox implements FocusHandler,
    BlurHandler, SelectionHandler<TermSuggestion>, KeyPressHandler {

  private String WATERMARK;

  private String previousValue;

  private ValueBoxBase<String> box;

  private Term selected;
  private List<Term> exactMatches = new ArrayList<Term>();

  private boolean isDefaultSuggestionDisplay = false;

  public TermSuggestBox() {
    this(new MultiWordSuggestOracle());
  }

  public TermSuggestBox(SuggestOracle oracle) {
    this(oracle, new TextBox());
  }

  public TermSuggestBox(SuggestOracle oracle, ValueBoxBase<String> box) {
    this(oracle, box, new DefaultSuggestionDisplay());

    this.isDefaultSuggestionDisplay = true;
  }

  public TermSuggestBox(SuggestOracle oracle, ValueBoxBase<String> box,
      SuggestionDisplay suggestDisplay) {
    super(oracle, box, suggestDisplay);
  }

  @Override
  protected void initWidget(Widget widget) {
    this.box = getValueBox();

    box.addKeyPressHandler(this);
    box.addFocusHandler(this);
    box.addBlurHandler(this);

    addHandler(this, SelectionEvent.getType());
    setAutoSelectEnabled(false);

    super.initWidget(box);
  }

  public Term getSelected() {
    return selected;
  }

  public List<Term> getExactMatches() {
    return exactMatches;
  }

  @Override
  public void onSelection(SelectionEvent<TermSuggestion> event) {
    TermSuggestion sug = event.getSelectedItem();
    selected = sug.getTerm();
    previousValue = selected.getTermString();
  }

  public void onExactMatchFound(List<Term> exactMatches) {
    this.exactMatches = exactMatches;
    if (exactMatches.size() == 1) {
      selected = exactMatches.get(0);
    }
  }

  @Override
  public void onKeyPress(KeyPressEvent event) {
    switch (event.getNativeEvent().getKeyCode()) {
      case KeyCodes.KEY_ESCAPE:
        if (isSuggestionListShowing()) {
          ((DefaultSuggestionDisplay) getSuggestionDisplay()).hideSuggestions();
        } else {
          setFocus(false);
        }
        break;
      default:
        updateSelected();
    }
  }

  private void updateSelected() {
    String value = getText();
    if (!value.equalsIgnoreCase(previousValue)) {
      selected = null;
    }
    previousValue = value;
  }

  @Override
  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(handler, KeyPressEvent.getType());
  }

  @Override
  public void onBlur(BlurEvent event) {
    enableWatermark();
  }

  @Override
  public void onFocus(FocusEvent event) {
    disableWatermark();
  }

  public void setWatermark(String text) {
    this.WATERMARK = text;

    box.setFocus(false);
    enableWatermark();
  }

  private void enableWatermark() {
    if (WATERMARK == null) {
      return;
    }

    String t = box.getText();
    if (t.length() == 0 || t.equalsIgnoreCase(WATERMARK)) {
      box.setText(WATERMARK);
      box.addStyleName("watermark");
    }
  }

  private void disableWatermark() {
    if (WATERMARK == null) {
      return;
    }

    String t = box.getText();
    box.removeStyleName("watermark");
    if (t.equals(WATERMARK)) {
      box.setText("");
    }
  }

  @Override
  public boolean isSuggestionListShowing() {
    if (!isDefaultSuggestionDisplay) {
      return false;
    }

    return getSuggestionDisplay()
        .isSuggestionListShowing();
  }

}
