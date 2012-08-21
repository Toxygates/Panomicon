package otgviewer.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.visualization.client.AbstractDrawOptions;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.Visualization;

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
