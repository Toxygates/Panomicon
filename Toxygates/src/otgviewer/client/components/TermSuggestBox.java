package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.TermSuggestOracle.TermSuggestion;
import t.common.shared.Term;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.Widget;

public class TermSuggestBox extends SuggestBox implements FocusHandler,
    BlurHandler, SelectionHandler<TermSuggestion>,
    ExactMatchHandler<Term>, KeyPressHandler {

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
    TermSuggestion sug = (TermSuggestion) event.getSelectedItem();
    selected = sug.getTerm();
    previousValue = selected.getTermString();
  }

  @Override
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

    return ((DefaultSuggestionDisplay) getSuggestionDisplay())
        .isSuggestionListShowing();
  }

}