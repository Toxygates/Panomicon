package t.viewer.client.components;

import java.util.*;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import t.common.shared.SharedUtils;
import t.viewer.client.Utils;

/**
 * A selection method for StackedListEditor that allows the user to edit a list as text, 
 * freely. Items are separated by commas or whitespace.
 */
public class FreeEdit extends SelectionMethod<String> {
  protected TextArea textArea = new ResizableTextArea(10, 45);
  private String lastText = "";
  private Timer t;
  private DockLayoutPanel dlp;
  private HorizontalPanel np;    

  public FreeEdit(StackedListEditor editor) {
    super(editor);
    dlp = new DockLayoutPanel(Unit.PX);
    initWidget(dlp);

    Label l = new Label("Search:");
    final SuggestBox sb = new SuggestBox(new SuggestOracle() {
      @Override
      public void requestSuggestions(Request request, Callback callback) {
        callback.onSuggestionsReady(request,
            new Response(parentSelector.getSuggestions(request.getQuery())));
      }
    });
    HorizontalPanel hp = Utils.mkHorizontalPanel(true, l, sb);
    np = Utils.mkWidePanel();
    np.add(hp);

    sb.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
      @Override
      public void onSelection(SelectionEvent<Suggestion> event) {
        Suggestion s = event.getSelectedItem();
        String selection = s.getDisplayString();
        String oldText = textArea.getText().trim();
        String newText = (!"".equals(oldText)) ? (oldText + "\n" + selection) : selection;
        textArea.setText(newText);
        refreshItems(true);
        sb.setText("");
      }
    });

    dlp.addNorth(np, 36);
    textArea.setSize("100%", "100%");
    dlp.add(textArea);
    t = new Timer() {
      @Override
      public void run() {
        refreshItems(false);
      }
    };

    textArea.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        lastText = textArea.getText();
        t.schedule(500);
      }
    });
  }

  private void refreshItems(boolean immediate) {
    final FreeEdit fe = this;
    // Without the immediate flag, only do the refresh action if
    // the text has been unchanged for 500 ms.
    if (immediate || lastText.equals(textArea.getText())) {
      String[] items = parseItems();
      Set<String> valid = parentSelector.validateItems(Arrays.asList(items));
      if (!parentSelector.getSelection().equals(valid)) {
        parentSelector.setSelection(valid, fe);
      }
      currentSelection = valid;
    }
  }

  @Override
  public String getTitle() {
    return "Edit/paste";
  }

  private String[] parseItems() {
    String s = textArea.getText();
    String[] split = s.split("\\s*[,\n]+\\s*");
    return split;
  }

  @Override
  public void setSelection(Collection<String> items) {
    super.setSelection(items);
    textArea.setText(SharedUtils.mkString(items, "\n"));
  }
}
