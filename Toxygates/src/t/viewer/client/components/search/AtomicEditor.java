/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package t.viewer.client.components.search;

import java.util.*;

import javax.annotation.Nullable;

import t.common.client.components.ItemSelector;
import t.common.shared.sample.search.AtomicMatch;
import t.common.shared.sample.search.MatchType;
import t.model.sample.Attribute;
import t.model.sample.BasicAttribute;
import t.viewer.client.Utils;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;

public class AtomicEditor extends MatchEditor {

  private ItemSelector<Attribute> paramSel;
  private ItemSelector<MatchType> typeSel;
  private TextBox textBox = new TextBox();
  
  final static Attribute UNDEFINED_ITEM =
      new BasicAttribute("undefined", "Undefined", false, null);
  
  public AtomicEditor(@Nullable MatchEditor parent, 
      final Collection<Attribute> parameters) {
    super(parent, parameters);
    
    HorizontalPanel hPanel = Utils.mkHorizontalPanel(true);
    initWidget(hPanel);
    hPanel.addStyleName("samplesearch-atomicpanel");
    
    paramSel = new ItemSelector<Attribute>() {
      @Override
      protected Attribute[] values() {
        List<Attribute> r = new ArrayList<Attribute>();
        r.add(UNDEFINED_ITEM);
        r.addAll(parameters);
        return r.toArray(new Attribute[0]);
      }      
      
      @Override
      protected String titleForValue(Attribute bp) {
        return bp.title();
      }
      
      @Override
      protected void onValueChange(Attribute selected) {
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

    typeSel.setSelected(MatchType.Low);
    hPanel.add(typeSel);

    hPanel.add(textBox);

    disable();
  }
  
  void disable() {
    typeSel.setVisible(false);
    textBox.setVisible(false);
  }
  
  void enable() {
    typeSel.setVisible(true);
    textBox.setVisible(typeSel.value().requiresValue());
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
