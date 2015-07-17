package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.TermSuggestOracle;
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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.Widget;

public class TermSuggestBox extends Composite implements FocusHandler,
    BlurHandler, SelectionHandler<SuggestOracle.Suggestion>, KeyPressHandler,
    HasKeyPressHandlers {

  private final Screen screen;
  private final String WATERMARK;

  private SuggestBox field;
  private String previousValue;

  private Term selected;
  private List<Term> exactMatches = new ArrayList<Term>();

  public TermSuggestBox(Screen screen, String watermark) {
    this.screen = screen;
    this.WATERMARK = watermark;

    initWidget(field);
  }

  @Override
  protected void initWidget(Widget widget) {
    TermSuggestOracle oracle = oracle();
    ValueBoxBase<String> tb = new TextBox();

    field = new SuggestBox(oracle, tb);
    field.addSelectionHandler(this);
    field.addKeyPressHandler(this);

    tb.addFocusHandler(this);
    tb.addBlurHandler(this);

    enableWatermark(tb);

    super.initWidget(field);
  }

  public String getText() {
    return field.getText();
  }

  public void setText(String text) {
    field.setText(text);
  }

  public Term getSelected() {
    return selected;
  }

  public List<Term> getExactMatches() {
    return exactMatches;
  }

  public void showSuggestion() {
    field.showSuggestionList();
  }

  @Override
  public void onSelection(SelectionEvent<Suggestion> event) {
    TermSuggestion sug = (TermSuggestion) event.getSelectedItem();
    selected = sug.getTerm();
    previousValue = selected.getTermString();
    System.out.println("Selected: " + selected.getTermString());
  }

  @Override
  public void onKeyPress(KeyPressEvent event) {
    String value = field.getText();
    if (!value.equalsIgnoreCase(previousValue)) {
      selected = null;
      System.out.println("Selected: reset to null");
    }
    previousValue = value;
  }

  private TermSuggestOracle oracle() {
    return new TermSuggestOracle(screen) {
      @Override
      public void onExactMatchFound(List<Term> exactMatches) {
        TermSuggestBox.this.exactMatches = exactMatches;
        System.out.println("ExactMatchesFound: " + exactMatches.size());
        if (exactMatches.size() == 1) {
          selected = exactMatches.get(0);
          System.out.println("ExactMatchesSelected: "
              + selected.getTermString());
        }
      }
    };
  }

  @Override
  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(handler, KeyPressEvent.getType());
  }

  @Override
  public void onBlur(BlurEvent event) {
    enableWatermark(field.getValueBox());
  }

  @Override
  public void onFocus(FocusEvent event) {
    disableWatermark(field.getValueBox());
  }

  private void enableWatermark(ValueBoxBase<String> textbox) {
    String t = textbox.getText();
    if (t.length() == 0 || t.equalsIgnoreCase(WATERMARK)) {
      textbox.setText(WATERMARK);
      textbox.addStyleName("watermark");
    }
  }

  private void disableWatermark(ValueBoxBase<String> textbox) {
    String t = textbox.getText();
    textbox.removeStyleName("watermark");
    if (t.equals(WATERMARK)) {
      textbox.setText("");
    }
  }

}
