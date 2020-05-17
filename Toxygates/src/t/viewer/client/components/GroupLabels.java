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

package t.viewer.client.components;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import t.common.shared.DataSchema;
import t.common.shared.sample.Group;
import t.viewer.client.ClientGroup;
import t.viewer.client.Utils;

import java.util.List;

public class GroupLabels extends Composite {

  protected List<ClientGroup> groups;
  protected DataSchema schema;
  private FlowPanel flowPanel;
  protected OTGScreen screen;

  final static int LABEL_MAX_LEN = 40;

  public GroupLabels(OTGScreen screen, DataSchema schema, List<ClientGroup> groups) {
    flowPanel = new FlowPanel();
    this.groups = groups;
    this.schema = schema;
    this.screen = screen;
    initWidget(flowPanel);
    showSmall();
  }

  protected String groupDetailString(Group g) {
    return ":" + g.getTriples(schema, 2, ", ");
  }

  private void show(List<ClientGroup> groups) {
    flowPanel.clear();
    for (Group group : groups) {
      FlowPanel groupPanel = new FlowPanel();
      groupPanel.addStyleName("statusBorder");
      String tip = group.tooltipText(schema);

      Label shortLabel = new Label(group.getName());
      Label longLabel = new Label(groupDetailString(group));
      Label[] labels = {shortLabel, longLabel};
      for (Label label : labels) {
        label.addStyleName(group.getStyleName());
        label.addStyleName("groupLabel");
        label.setTitle(tip);
        groupPanel.add(label);
      }

      Utils.addAndFloatLeft(flowPanel, groupPanel);
    }
  }

  private void showAll() {
    show(groups);
    if (groups.size() > 5) {
      Button b = new Button("Hide", new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showSmall();
        }
      });
      Utils.addAndFloatLeft(flowPanel, b);
    }
    screen.resizeInterface();
  }

  private void showSmall() {
    if (groups.size() > 5) {
      List<ClientGroup> gs = groups.subList(0, 5);
      show(gs);
      Button b = new Button("Show all", new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showAll();
        }
      });
      Utils.addAndFloatLeft(flowPanel, b);
    } else {
      show(groups);
    }
    screen.resizeInterface();
  }

}
