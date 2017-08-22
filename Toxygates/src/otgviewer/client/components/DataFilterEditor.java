/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otgviewer.client.components;

import java.util.*;
import java.util.logging.Logger;

import t.common.shared.SharedUtils;
import t.model.SampleClass;
import t.viewer.client.rpc.SampleServiceAsync;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

public class DataFilterEditor extends DataListenerWidget {
  List<SampleClass> sampleClasses = new ArrayList<SampleClass>();
  final SCListBox[] selectors;
  private final SampleServiceAsync sampleService;
  private final String[] parameters;
  protected final Logger logger;

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

    void trySelect(String item) {
      for (int i = 0; i < getItemCount(); i++) {
        if (getItemText(i).equals(item)) {
          setSelectedIndex(i);
          return;
        }
      }
    }

    void setItemsFrom(List<SampleClass> scs, String key) {
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
      SampleClass r = new SampleClass();
      boolean allSet = true;
      for (int i = 0; i < selectors.length; ++i) {
        String x = selectors[i].getSelected();
        if (x == null) {
          allSet = false;
        } else {
          r.put(parameters[i], x);
        }
      }

      if (allSet && emitChange) {
//        logger.info("Propagate change to " + r.toString());
        changeSampleClass(r);
      }
    }
  }

  public DataFilterEditor(Screen screen) {
    HorizontalPanel hp = new HorizontalPanel();
    initWidget(hp);
    logger = SharedUtils.getLogger("dfeditor");
    sampleService = screen.manager.sampleService();

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

  public void update() {
    sampleService.sampleClasses(new PendingAsyncCallback<SampleClass[]>(this,
        "Unable to obtain sample classes from server") {
      @Override
      public void handleSuccess(SampleClass[] result) {
        setAvailable(result);
      }
    });
  }

  public void setAvailable(SampleClass[] sampleClasses) {
    logger.info("Received " + sampleClasses.length + " sample classes");
    if (sampleClasses.length > 0) {
      logger.info(sampleClasses[0].toString() + " ...");
    }
    this.sampleClasses = Arrays.asList(sampleClasses);
    selectors[0].setItemsFrom(this.sampleClasses, parameters[0]);
    changeFrom(0, true); // Propagate the constraint
  }

  @Override
  public void sampleClassChanged(SampleClass sc) {
    // do NOT call superclass method. Prevent signal from being passed on.
    chosenSampleClass = sc;

    for (int i = 0; i < selectors.length; ++i) {
      selectors[i].trySelect(sc.get(parameters[i]));      
      changeFrom(i, false);
    }
  }
}
