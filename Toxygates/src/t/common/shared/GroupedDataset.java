/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package t.common.shared;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * This class is intended mainly for grouping shared datasets, which will have
 * the same user title but different IDs.
 * @author johan
 */

@SuppressWarnings("serial")
public class GroupedDataset extends Dataset {

  private List<Dataset> subDatasets;
  
  public GroupedDataset(String title, String description, List<Dataset> subDatasets) {
    super(title, description, "", new Date(), "", 0);
    this.subDatasets = subDatasets;
  }
  
  @Override
  public Collection<Dataset> getSubDatasets() {
    return subDatasets;    
  }
}
