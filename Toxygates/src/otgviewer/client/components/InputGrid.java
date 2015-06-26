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

package otgviewer.client.components;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;

public class InputGrid extends Composite {

	private TextBox[] inputs;
	
	public InputGrid(String... titles) {
		inputs = new TextBox[titles.length];		
		
		Panel p = new SimplePanel();
		initWidget(p);
		Grid g = new Grid(titles.length, 2);
		p.add(g);
		
		for (int i = 0; i < titles.length; ++i) { 			
			inputs[i] = initTextBox(i);
			inputs[i].setWidth("20em");
			g.setWidget(i, 0, new Label(titles[i]));
			g.setWidget(i, 1, inputs[i]);						
		}
	}
	
	protected TextBox initTextBox(int i) {
		return new TextBox();
	}
	
	public String getValue(int i) {
		return inputs[i].getValue();
	}
}
