/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import java.util.Collection;

import javax.annotation.Nullable;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

import t.model.sample.Attribute;

abstract public class MatchEditor extends Composite {

  protected Collection<Attribute> parameters;
  
  protected @Nullable MatchEditor parent;
  
  private boolean signalled = false;
  
  public MatchEditor(@Nullable MatchEditor parent, Collection<Attribute> parameters) {
    super();
    this.parameters = parameters;
    this.parent = parent;
  }
  
  public void updateParameters(Collection<Attribute> parameters) {
    this.parameters = parameters;
  }
  
  void signalEdit() {
    if (parent != null) {
      if (!signalled) {
        signalled = true;
        parent.childFirstEdit();        
      }
      parent.childEdit();
    }
  }
  
  /**
   * Invoked by child editors when they are edited for the first time.
   */
  void childFirstEdit() {
    expand();
    signalEdit();
  }
  
  /**
   * Invoked by child editors when they are edited.
   */
  void childEdit() {
    signalEdit();
  }
  
  /**
   * Expand this editor by adding more children.
   */
  protected void expand() { }
  
  protected Label mkLabel(String string) {
    Label l = new Label(string);
    l.addStyleName("samplesearch-label");
    return l;
  }

}
