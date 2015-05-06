package otgviewer.client.components;

import otgviewer.client.Utils;
import t.common.shared.sample.ExpressionRow;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;


public class ExpressionColumn extends Column<ExpressionRow, String> {
	final int i;

	private static Utils.Templates TEMPLATES = GWT.create(Utils.Templates.class);

	/**
	 * @param tc
	 * @param matColumn column index in the underlying data matrix (e.g. in ManagedMatrixInfo)
	 */
	public ExpressionColumn(TextCell tc, int matColumn) {
		super(tc);
		this.i = matColumn;	
	}

	public int matrixColumn() {
		return i;
	}
	
	public String getValue(ExpressionRow er) {
		if (er != null) {			
			if (!er.getValue(i).getPresent()) {
				return "(absent)";
			} else {
				return Utils.formatNumber(er.getValue(i).getValue());
			}
		} else {
			return "";
		}
	}
	
	@Override
	public void render(final Context context, final ExpressionRow object, 
			final SafeHtmlBuilder sb) {
		if (object != null) {
			final String tooltip = object.getValue(i).getTooltip();
			sb.append(TEMPLATES.startToolTip(tooltip));
			super.render(context, object, sb);
			sb.append(TEMPLATES.endToolTip());
		} else {
			super.render(context, object, sb);
		}
	}
}