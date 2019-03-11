/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otg.viewer.client.screen.data;

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;

import otg.viewer.client.components.OTGScreen;
import t.common.shared.SharedUtils;
import t.model.SampleClass;
import t.model.sample.Attribute;

public class DataFilterEditor extends Composite {
  List<SampleClass> sampleClasses = new ArrayList<SampleClass>();
  final SCListBox[] selectors;
  private final Attribute[] parameters;
  protected final Logger logger;
  private Delegate delegate;
  
  // This default value is necessary to prevent null pointer exceptions
  protected SampleClass chosenSampleClass = new SampleClass();
  
  public interface Delegate {
    void dataFilterEditorSampleClassChanged(SampleClass sc);
  }

  class SCListBox extends ListBox {
    int idx;

    SCListBox(int idx) {
      this.idx = idx;
    }

    void setItems(List<String> items) {
      String oldSel = getSelected();
      clear();
      for (String i : items) {
        addItem(i);
      }

      if (oldSel != null && items.indexOf(oldSel) != -1) {
        trySelect(oldSel);
      } else if (items.size() > 0) {
        setSelectedIndex(0);
      }
    }

    boolean trySelect(String item) {
      for (int i = 0; i < getItemCount(); i++) {
        if (getItemText(i).equals(item)) {
          setSelectedIndex(i);
          return true;
        }
      }
      return false;
    }

    void setItemsFrom(List<SampleClass> scs, Attribute key) {
      setItems(new ArrayList<String>(SampleClass.collect(scs, key)));
    }

    String getSelected() {
      int i = getSelectedIndex();
      if (i != -1) {
        return getItemText(i);
      } else {
        return null;
      }
    }
  }

  void changeFrom(int sel, boolean emitChange) {
    List<SampleClass> selected = sampleClasses;
    // Get the selected values on the left of, and including, this one
    for (int i = 0; i <= sel; ++i) {
      String sval = selectors[i].getSelected();
      if (sval != null) {
        selected = SampleClass.filter(selected, parameters[i], sval);
//        logger.info("Filtered to " + selected.size());
      }
    }
    // Constrain the selectors to the right of this one
    for (int i = sel + 1; i < selectors.length; ++i) {
      selectors[i].setItemsFrom(selected, parameters[i]);
    }

    if (sel < selectors.length - 1) {
      changeFrom(sel + 1, emitChange);
    } else if (sel == selectors.length - 1) {
      SampleClass sampleClass = new SampleClass();
      boolean allSet = true;
      for (int i = 0; i < selectors.length; ++i) {
        String x = selectors[i].getSelected();
        if (x == null) {
          allSet = false;
        } else {
          sampleClass.put(parameters[i], x);
        }
      }

      if (allSet && emitChange) {
//        logger.info("Propagate change to " + r.toString());
        chosenSampleClass = sampleClass;
        delegate.dataFilterEditorSampleClassChanged(sampleClass);
      }
    }
  }

  public DataFilterEditor(OTGScreen screen, Delegate delegate) {
    this.delegate = delegate;
    HorizontalPanel hp = new HorizontalPanel();
    initWidget(hp);
    logger = SharedUtils.getLogger("dfeditor");

    parameters = screen.schema().macroParameters();
    selectors = new SCListBox[parameters.length];
    for (int i = 0; i < parameters.length; ++i) {
      selectors[i] = new SCListBox(i);
      hp.add(selectors[i]);
      final int sel = i;
      selectors[i].addChangeHandler(new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
          changeFrom(sel, true);
        }
      });
    }
  }

  public void setAvailable(SampleClass[] sampleClasses) {
    this.sampleClasses = Arrays.asList(sampleClasses);
    selectors[0].setItemsFrom(this.sampleClasses, parameters[0]);
    
    // Attempt to preserve the already chosen sample class by sequentially setting
    // each selector accordingly. 
    boolean oldSampleClassMatchesConstraints = true;
    for (int i = 0; i < selectors.length; ++i) {
      oldSampleClassMatchesConstraints = 
          selectors[i].trySelect(chosenSampleClass.get(parameters[i]));
      if (oldSampleClassMatchesConstraints) {
        changeFrom(i, false);
      }
    }
    // If the above fails, we start at the top and generate a fresh set of values
    // that match the constraints.
    if (!oldSampleClassMatchesConstraints) {
      changeFrom(0, true); 
    }
  }

  // Incoming message from FilterTools
  public void sampleClassChanged(SampleClass sc) {
    chosenSampleClass = sc;

    for (int i = 0; i < selectors.length; ++i) {
      selectors[i].trySelect(sc.get(parameters[i]));      
      changeFrom(i, false);
    }
  }
}
