package t.viewer.client.components.search;

import java.util.Collection;

import javax.annotation.Nullable;

import t.common.client.components.ItemSelector;
import t.common.shared.sample.search.AtomicMatch;
import t.common.shared.sample.search.MatchType;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

public class AtomicEditor extends MatchEditor {

  private ListBox paramSel;
  private ItemSelector<MatchType> typeSel;
  
  final static String UNDEFINED_ITEM = "Undefined";
  
  public AtomicEditor(@Nullable MatchEditor parent, Collection<String> parameters) {
    super(parent, parameters);
    
    HorizontalPanel hp = new HorizontalPanel();
    initWidget(hp);
    hp.addStyleName("samplesearch-atomicpanel");
    
    paramSel = new ListBox();
    paramSel.addItem(UNDEFINED_ITEM);
    
    for (String param: parameters) {
      paramSel.addItem(param);
    }
    paramSel.addChangeHandler(new ChangeHandler() {      
      @Override
      public void onChange(ChangeEvent arg0) {
        if (paramSel.getSelectedItemText().equals(UNDEFINED_ITEM)) {
          disable();
        } else {
          enable();
        }
        signalEdit();        
      }
    });
    
    hp.add(paramSel);
    
    typeSel = new ItemSelector<MatchType>() {
      protected MatchType[] values() {
        return MatchType.values();
      }
    };    
    hp.add(typeSel);
    disable();
  }
  
  void disable() {
    typeSel.listBox().setEnabled(false);
  }
  
  void enable() {
    typeSel.listBox().setEnabled(true);
  }
  
  public @Nullable AtomicMatch getCondition() {
    if (paramSel.getSelectedItemText().equals(UNDEFINED_ITEM)) {
      return null;
    }
    return new AtomicMatch(paramSel.getSelectedItemText(), typeSel.value(), null);
  }

}
