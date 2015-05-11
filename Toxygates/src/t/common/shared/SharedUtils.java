package t.common.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

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
	
	public static String mkString(String beforeEach, String[] ar, String afterEach) {
		return beforeEach + mkString(ar, afterEach + beforeEach) + afterEach;
	}
	
	public static String mkString(Collection<? extends Object> cl, String separator) {
		List<String> ss = new ArrayList<String>();
		for (Object o: cl) {
			if (o == null) {
				ss.add("null");
			} else {
				ss.add(o.toString());
			}
		}
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
	
	public static Logger getLogger() {
		return getLogger("default");		
	}
	
	public static Logger getLogger(String suffix) {
		return Logger.getLogger("jp.level-five.tframework." + suffix);
	}
}
