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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.visualization.client.AbstractDrawOptions;
import com.google.gwt.visualization.client.visualizations.Visualization;

/**
 * A heatmap that wraps a Javascript heatmap developed by systemsbiology.org.
 * Note that war/toxygates.html must be modified to load the necessary javascript API
 * if this is to be used.
 * For more info on the bio heat map, see this page
 * http://informatics.systemsbiology.net/visualizations/heatmap/bioheatmap.html
 * @author johan
 *
 */
public class BioHeatMap extends Visualization<BioHeatMap.Options> {

	public static class Options extends AbstractDrawOptions {
		protected Options() {}
		
		public static Options create() {
			return JavaScriptObject.createObject().cast();
		}
		
		public final native void setWidth(int width) /*-{
	      this.width = width;
	    }-*/;
		
		public final native void setHeight(int height) /*-{
			this.height = height;
		}-*/;
	}
	
	private Options options;
	
	public BioHeatMap(Options options) {
		this.options = options;
	}
	
	public native JavaScriptObject createJso(Element parent) /*-{
		return new $wnd.org.systemsbiology.visualization.BioHeatMap(parent);
	}-*/;
	
//	public native void draw(DataTable table) /*-{
//		this.draw(table);
//	}-*/;
}
