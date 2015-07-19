package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.TermSuggestOracle.TermSuggestion;
import t.common.shared.Term;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.Widget;

public class TermSuggestBox extends SuggestBox implements FocusHandler,
    BlurHandler, SelectionHandler<SuggestOracle.Suggestion>, KeyPressHandler {

  private String WATERMARK;

  private String previousValue;

  private ValueBoxBase<String> box;

  private Term selected;
  private List<Term> exactMatches = new ArrayList<Term>();

  public TermSuggestBox() {
    this(new MultiWordSuggestOracle());
  }

  public TermSuggestBox(SuggestOracle oracle) {
    this(oracle, new TextBox());
  }

  public TermSuggestBox(SuggestOracle oracle, ValueBoxBase<String> box) {
    this(oracle, box, new DefaultSuggestionDisplay());
  }

  public TermSuggestBox(SuggestOracle oracle, ValueBoxBase<String> box,
      SuggestionDisplay suggestDisplay) {
    super(oracle, box, suggestDisplay);
  }

  @Override
  protected void initWidget(Widget widget) {
    this.box = getValueBox();

    addSelectionHandler(this);
    box.addFocusHandler(this);
    box.addBlurHandler(this);

    super.initWidget(box);
  }

  public Term getSelected() {
    return selected;
  }

  public List<Term> getExactMatches() {
    return exactMatches;
  }

  @Override
  public void onSelection(SelectionEvent<Suggestion> event) {
    TermSuggestion sug = (TermSuggestion) event.getSelectedItem();
    selected = sug.getTerm();
    previousValue = selected.getTermString();
  }

  @Override
  public void onKeyPress(KeyPressEvent event) {
    String value = getText();
    if (!value.equalsIgnoreCase(previousValue)) {
      selected = null;
    }
    previousValue = value;
  }

  // @Override
  // public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
  // return addDomHandler(handler, KeyPressEvent.getType());
  // }

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

}
