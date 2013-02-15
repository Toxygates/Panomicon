package otgviewer.shared;

import java.util.ArrayList;
import java.util.Arrays;
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
		return mkString(ar, "");
	}
	
	public static String mkString(String[] ar, String separator) {
		return mkString(Arrays.asList(ar), separator);		
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
		if (r.length() > 0) {
			return r.substring(0, r.length() - separator.length()); //remove final separator
		} else {
			return r;
		}
	}
	
	public static List<DataColumn> asColumns(List<Group> groups) {		
		List<DataColumn> r = new ArrayList<DataColumn>(groups.size());	
		for (Group g: groups) {
			r.add(g);
		}		
		return r;
	}
	
	
}
