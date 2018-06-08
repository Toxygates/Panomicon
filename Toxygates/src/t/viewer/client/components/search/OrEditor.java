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

import t.common.shared.sample.search.*;
import t.model.sample.Attribute;
import t.viewer.client.Utils;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class OrEditor extends MatchEditor {

  private List<AtomicEditor> atomicEditors = new ArrayList<AtomicEditor>();
  
  private HorizontalPanel panel = Utils.mkHorizontalPanel(true);
  
  public OrEditor(@Nullable MatchEditor parent, Collection<Attribute> parameters) {
    super(parent, parameters);
    initWidget(panel);    
    AtomicEditor a = newAtomic();
    atomicEditors.add(a);
    panel.add(a);
    panel.addStyleName("samplesearch-orpanel");
  }

  public @Nullable MatchCondition getCondition() {
    List<AtomicMatch> atomics = new ArrayList<AtomicMatch>();
    for (AtomicEditor ac: atomicEditors) {
      AtomicMatch match = ac.getCondition();
      if (match != null) {
        atomics.add(match);
      }
    }
    if (atomics.size() > 1) {
      return new OrMatch(atomics);
    } else if (atomics.size() == 1) {
      return atomics.get(0);
    } else {
      return null;
    }
  }
  
  AtomicEditor newAtomic() {
    return new AtomicEditor(this, parameters);
  }
  
  @Override
  protected void expand() {
    super.expand();
    AtomicEditor a = newAtomic();
    atomicEditors.add(a);    
    Label l = mkLabel("OR");
    panel.add(l);
    panel.add(a);    
  }
}
