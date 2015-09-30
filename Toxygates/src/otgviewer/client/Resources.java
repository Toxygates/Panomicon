/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package otgviewer.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundle {

	@Source("images/16_statistics.png")
	ImageResource chart();
	
	@Source("images/16_close.png")
	ImageResource close();
	
	@Source("images/16_info.png")
	ImageResource info();
	
	@Source("images/16_search.png")
	ImageResource magnify();
	
	@Source("images/16_faq.png")
	ImageResource help();
	
	@Source("images/12_filter.png")
	ImageResource filter();
	
	@Source("help/default.html")
	TextResource defaultHelpHTML();
	
	@Source("help/groupDefinition.png")
	ImageResource groupDefinitionHelp();
	
	@Source("help/groupDefinition.html")
	TextResource groupDefinitionHTML();	
	
	@Source("help/probeSelection.png")
	ImageResource probeSelectionHelp();
	
	@Source("help/probeSelection.html")
	TextResource probeSelectionHTML();
	
	@Source("help/dataDisplay.html")
	TextResource dataDisplayHTML();
	
	@Source("help/dataDisplay.png")
	ImageResource dataDisplayHelp();
	
	@Source("help/compoundRanking.html")
	TextResource compoundRankingHTML();
	
	@Source("help/compoundRanking.png")
	ImageResource compoundRankingHelp();
	
	@Source("help/start.html")
	TextResource startHTML();
	
	@Source("help/about.html")
	TextResource aboutHTML();
	
	@Source("help/about.png")
	ImageResource about();
	
	@Source("help/versions.html")
	TextResource versionHTML();
	
}
