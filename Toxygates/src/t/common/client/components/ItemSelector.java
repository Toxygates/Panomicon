/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.client.components;

import java.util.Arrays;

import javax.annotation.Nullable;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;

/**
 * A convenience widget for selecting among some values of type T.
 * Values of T must be uniquely mapped to strings.
 * By default, toString is used to establish this mapping.
 * 
 * @param <T>
 */
public abstract class ItemSelector<T> extends Composite {

  private ListBox lb = new ListBox();
 
  public ItemSelector(T[] values) {
    initWidget(lb);
    setup(values);
  }
  
  public ItemSelector() {
    initWidget(lb);
    setup(values());
  }
  
  private void setup(T[] values) {
    lb.setVisibleItemCount(1);
    for (T t : values) {
      lb.addItem(titleForValue(t));
    }
    lb.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        onValueChange(value());
      }
    });
  }

  public ListBox listBox() {
    return lb;
  }

  public @Nullable T value() {
    if (lb.getSelectedIndex() == -1) {
      return null;
    }
    return valueForTitle(lb.getItemText(lb.getSelectedIndex()));
  }

  public void reset() {
    lb.setSelectedIndex(0);
  }

  protected abstract T[] values();

  /**
   * T to String mapping
   * @param t
   * @return
   */
  protected String titleForValue(T t) {
    return t.toString();
  }
  
  /**
   * String to T mapping - must be the reverse of the above.
   * By default all the forward mappings are searched (suitable only
   * for a smaller number of values)
   * @param title
   * @return
   */
  protected @Nullable T valueForTitle(String title) {
    for (T t : values()) {
      if (title.equals(titleForValue(t))) {
        return t;
      }
    }
    return null;
  }
  
  public void setSelected(T t) {
    for (int i = 0; i < values().length; i++) {
      if (values()[i].equals(t)) {
        lb.setSelectedIndex(i);
        return;
      }
    }    
  }

  protected void onValueChange(T selected) {}
}
