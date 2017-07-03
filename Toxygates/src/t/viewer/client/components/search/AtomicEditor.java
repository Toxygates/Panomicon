package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import t.common.client.components.ItemSelector;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.StringBioParamValue;
import t.common.shared.sample.search.AtomicMatch;
import t.common.shared.sample.search.MatchType;

import com.google.gwt.user.client.ui.HorizontalPanel;

public class AtomicEditor extends MatchEditor {

  private ItemSelector<BioParamValue> paramSel;
  private ItemSelector<MatchType> typeSel;
  
  final static BioParamValue UNDEFINED_ITEM = 
      new StringBioParamValue("undefined", "Undefined", "", "");
  
  public AtomicEditor(@Nullable MatchEditor parent, 
      final Collection<BioParamValue> parameters) {
    super(parent, parameters);
    
    HorizontalPanel hp = new HorizontalPanel();
    initWidget(hp);
    hp.addStyleName("samplesearch-atomicpanel");
    
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
    if (paramSel.value().equals(UNDEFINED_ITEM)) {
      return null;
    }
    return new AtomicMatch(paramSel.value(), typeSel.value(), null);
  }

}
