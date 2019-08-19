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

package otg.viewer.client.charts;

import java.util.LinkedList;
import java.util.List;

import otg.viewer.client.components.OTGScreen;
import t.common.shared.ValueType;
import t.viewer.client.ClientGroup;

/**
 * Parameters for chart creation and display.
 */
public class ChartParameters {
  public final OTGScreen screen;
  public final List<ClientGroup> groups;
  public final ValueType vt;
  public final String title;

  public ChartParameters(OTGScreen screen, ValueType vt, String title) {
    this.screen = screen;
    this.groups = new LinkedList<ClientGroup>();
    this.vt = vt;
    this.title = title;
  }

  public ChartParameters(OTGScreen screen, List<ClientGroup> groups, ValueType vt, String title) {
    this.screen = screen;
    this.groups = groups;
    this.vt = vt;
    this.title = title;
  }
}
