package otgviewer.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SharedUtils {
	public static <T> int indexOf(T[] haystack, T needle) {
		for (int i = 0; i < haystack.length; ++i) {
			if (haystack[i].equals(needle)) {
				return i;
			}
		}
		return -1;
	}

	public static <T> int indexOf(List<T> haystack, T needle) {
		for (int i = 0; i < haystack.size(); ++i) {
			if (haystack.get(i).equals(needle)) {
				return i;
			}
		}
		return -1;
	}

	public static String mkString(String[] ar) {
		StringBuilder sb = new StringBuilder();
		for (String s: ar) {
			sb.append(s);
		}
		return sb.toString();
	}
	
	public static String mkString(Collection<String> cl, String separator) {
		List<String> ss = new ArrayList<String>(cl);
		java.util.Collections.sort(ss);
		StringBuilder sb = new StringBuilder();		
		for (String s: ss) {
			sb.append(s);
			sb.append(separator);
		}
		String r = sb.toString();
		return r.substring(0, r.length() - 2); //remove final separator
	}
	
	public static List<DataColumn> asColumns(List<Group> groups) {		
		List<DataColumn> r = new ArrayList<DataColumn>(groups.size());	
		for (Group g: groups) {
			r.add(g);
		}		
		return r;
	}
	
	
}
