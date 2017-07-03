package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;

import t.common.client.components.ItemSelector;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.StringBioParamValue;
import t.common.shared.sample.search.AtomicMatch;
import t.common.shared.sample.search.MatchType;

public class AtomicEditor extends MatchEditor {

  private ItemSelector<BioParamValue> paramSel;
  private ItemSelector<MatchType> typeSel;
  private TextBox textBox = new TextBox();
  
  final static BioParamValue UNDEFINED_ITEM = 
      new StringBioParamValue("undefined", "Undefined", "", "");
  
  public AtomicEditor(@Nullable MatchEditor parent, 
      final Collection<BioParamValue> parameters) {
    super(parent, parameters);
    
    HorizontalPanel hPanel = new HorizontalPanel();
    initWidget(hPanel);
    hPanel.addStyleName("samplesearch-atomicpanel");
    
    paramSel = new ItemSelector<BioParamValue>() {
      @Override
      protected BioParamValue[] values() {
        List<BioParamValue> r = new ArrayList<BioParamValue>();
        r.add(UNDEFINED_ITEM);
        r.addAll(parameters);
        return r.toArray(new BioParamValue[0]);
      }      
      
      @Override
      protected String titleForValue(BioParamValue bp) {
        return bp.label();
      }
      
      @Override
      protected void onValueChange(BioParamValue selected) {
        if (selected.equals(UNDEFINED_ITEM)) {
          disable();
        } else {
          enable();
        }
        signalEdit();
      }
    };
    
    hPanel.add(paramSel);
    
    typeSel = new ItemSelector<MatchType>() {
      @Override
      protected MatchType[] values() {
        return MatchType.values();
      }

      @Override
      protected void onValueChange(MatchType selected) {
        textBox.setVisible(selected.requiresValue());
      }
    };    
    hPanel.add(typeSel);

    hPanel.add(textBox);
    textBox.setVisible(typeSel.value().requiresValue());
    disable();
  }
  
  void disable() {
    typeSel.listBox().setEnabled(false);
  }
  
  void enable() {
    typeSel.listBox().setEnabled(true);
  }
  
  public @Nullable AtomicMatch getCondition() {
    if (paramSel.value().equals(UNDEFINED_ITEM)) {
      return null;
    }

    if (typeSel.value().requiresValue()) {
      try {
        Double doubleValue = Double.parseDouble(textBox.getValue());
        return new AtomicMatch(paramSel.value(), typeSel.value(), doubleValue);
      } catch (NumberFormatException e) {
        Window.alert("Invalid number entered in search condition.");
        return null;
      }
    } else {
      return new AtomicMatch(paramSel.value(), typeSel.value(), null);
    }
  }

}
