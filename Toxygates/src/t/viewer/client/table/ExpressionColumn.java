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

package t.viewer.client.table;

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