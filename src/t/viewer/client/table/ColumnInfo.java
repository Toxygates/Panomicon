package t.viewer.client.table;

import javax.annotation.Nullable;

import t.viewer.client.table.RichTable.HideableColumn;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class ColumnInfo { 

	static final String DEFAULT_COL_WIDTH = "12em";
	
	private String title, tooltip, width;
	private @Nullable String cellStyleNames;
	//TODO hideable may not be needed here
	private boolean sortable, hideable, defaultSortAsc, filterable;
	
	public ColumnInfo(String title, String tooltip, boolean sortable,
			boolean hideable, String width, String cellStyleNames,
			boolean defaultSortAsc, boolean filterable) {
		this.title = title;
		this.sortable = sortable;
		this.hideable = hideable;
		this.width = width;
		this.tooltip = tooltip;
		this.cellStyleNames = cellStyleNames;
		this.defaultSortAsc = defaultSortAsc;
		this.filterable = filterable;
	}
	
	public ColumnInfo(ColumnInfo ci) {
		this(ci.title(), ci.tooltip(), ci.sortable(),
				ci.hideable(), ci.width(), ci.cellStyleNames(),
				ci.defaultSortAsc(), ci.filterable());				
	}
	
	public ColumnInfo(String title, String tooltip,
			boolean sortable, boolean hideable, boolean filterable) {
		this(title, tooltip, sortable, hideable, DEFAULT_COL_WIDTH,
				null, false, filterable);
	}
	
	public ColumnInfo(HideableColumn hc, boolean sortable) {
		this(hc.name(), hc.name(), sortable, true, false);
		this.width = hc.width();
	}

	public SafeHtml headerHtml() {
		 return SafeHtmlUtils.fromSafeConstant("<span title=\"" + tooltip + 
				 "\">" + title + "</span>");
	}
	
	/**
	 * Adjust title and tooltip so that title fits inside
	 * the maximum length.
	 * @param maxLength
	 * @return
	 */
	public ColumnInfo trimTitle(int maxLength) {
		String ntitle = title;
		String ntooltip = tooltip;
		if (ntitle.length() > maxLength) {			
			if (!ntooltip.equals(ntitle)) {
				ntooltip = ntitle + " (" + ntooltip + ")";
			}
			ntitle = ntitle.substring(0, maxLength) + "...";
		}
		return new ColumnInfo(ntitle, ntooltip, sortable,
				hideable, width, cellStyleNames,
				defaultSortAsc, filterable);
	}

	public boolean filterable() { return filterable; }
	public String title() { return title; }	
	public String tooltip() { return tooltip; }	
	public boolean sortable() { return sortable; }	
	public boolean hideable() { return hideable; }
	
	public String cellStyleNames() { return cellStyleNames; }	
	public void setCellStyleNames(String v) { cellStyleNames = v; }
	
	public boolean defaultSortAsc() { return defaultSortAsc; }	
	public void setDefaultSortAsc(boolean v) { defaultSortAsc = v; }
	
	public String width() { return width; }
	public void setWidth(String v) { width = v; }

}
