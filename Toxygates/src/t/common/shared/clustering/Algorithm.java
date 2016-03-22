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

package t.common.shared.clustering;

import javax.annotation.Nullable;

/**
 * Definitions of available clusterings
 */
public enum Algorithm {
  HIERARCHICAL("Hierarchical", new String[] {"LV", "LN", "SP"}, new String[] {"K"});

  private String title;
  private String[] clusterings;
  private String[] params;

  private Algorithm(String title, String[] clusterings, @Nullable String[] params) {
    this.title = title;
    this.clusterings = clusterings;
    this.params = params;
  }

  public String getTitle() {
    return title;
  }

  public String[] getClusterings() {
    return clusterings;
  }

  @Nullable
  public String[] getParams() {
    return params;
  }
  
}
