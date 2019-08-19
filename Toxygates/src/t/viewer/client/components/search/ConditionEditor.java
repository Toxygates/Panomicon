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

import t.common.shared.sample.search.MatchCondition;
import t.model.sample.Attribute;

import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Visual editor for sample match conditions.
 * A condition is a conjunction of disjunctions. 
 * Conjunctions are stacked vertically, disjunctions horizontally.
 */
public class ConditionEditor extends MatchEditor {

  private SimplePanel panel = new SimplePanel();
  private AndEditor root;
  
  public ConditionEditor(Collection<Attribute> parameters) {
    super(null, parameters);
    clear();   
    initWidget(panel);
    panel.addStyleName("samplesearch-rootpanel");
  }
  
  /**
   * Get the current condition
   * @return
   */
  public MatchCondition getCondition() {
    return root.getCondition();
  }
  
  AndEditor newAnd() {
    return new AndEditor(this, parameters);
  }
  
  /**
   * Reset the condition.
   */
  public void clear() {
    panel.clear();
    root = newAnd();
    panel.add(root);
  }

  @Override
  void signalEdit() {
    super.signalEdit();
    conditionChanged();
  }
  
  /**
   * Invoked when the condition has changed.
   */
  public void conditionChanged() {
    
  }
}
