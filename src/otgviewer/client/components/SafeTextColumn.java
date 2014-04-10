package otgviewer.client.components;

import com.google.gwt.user.cellview.client.TextColumn;

/**
 * Motivation:
 * This issue http://code.google.com/p/google-web-toolkit/issues/detail?id=7030
 * 
 * @param <T>
 */
abstract public class SafeTextColumn<T> extends TextColumn<T> {

	@Override
	public String getValue(T x) {
		if (x != null) {
			return safeGetValue(x);
		}
		return "";
	}
	
	abstract public String safeGetValue(T x);

}
