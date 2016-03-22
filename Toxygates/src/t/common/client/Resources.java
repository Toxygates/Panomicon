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

package t.common.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundle {

  @Source("images/16_statistics.png")
  ImageResource chart();
  
  @Source("images/16_close.png")
  ImageResource close();
  
  @Source("images/16_faq.png")
  ImageResource help();
  
  @Source("images/16_info.png")
  ImageResource info();

  @Source("images/16_search.png")
  ImageResource magnify();
  
  @Source("images/12_filter.png")
  ImageResource filter();
  
  @Source("images/12_filter_blue_2.png")
  ImageResource filterActive();
}
