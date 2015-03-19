package t.common.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import otgviewer.shared.Group;

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
	
	/*
	 * The mapper methods are not currently used. Consider retiring.
	 */
	
	
	public static interface Mapper<T, U> {
		public T map(U u);
	}
	
	public static interface FlatMapper<T, U> {
		public Collection<T> map(U u);
	}
	
	public static <T, U> List<T> map(Collection<U> us, Mapper<T, U> mapper) {
		List<T> r = new ArrayList<T>();
		for (U u: us) {
			r.add(mapper.map(u));
		}
		return r;
	}
	
	public static <T, U> List<T> flatMap(Collection<U> us, FlatMapper<T, U> mapper) {
		List<T> r = new ArrayList<T>();
		for (U u: us) {
			r.addAll(mapper.map(u));
		}
		return r;
	}
	
	/*
	 * We need this method and the similar ones below since
	 * GWT doesn't support Arrays.copyOf.
	 * What to do??
	 */	
	public static String[] extend(String[] data, String add) {
		String[] r = new String[data.length + 1];
		for (int i = 0; i < data.length; ++i) {
			r[i] = data[i];
		}
		r[data.length] = add;
		return r;
	}
	
	public static boolean[] extend(boolean[] data, boolean add) {
		boolean[] r = new boolean[data.length + 1];
		for (int i = 0; i < data.length; ++i) {
			r[i] = data[i];
		}
		r[data.length] = add;
		return r;
	}
	
	public static Double[] extend(Double[] data, Double add) {
		Double[] r = new Double[data.length + 1];
		for (int i = 0; i < data.length; ++i) {
			r[i] = data[i];
		}
		r[data.length] = add;
		return r;
	}
	
	public static Group[] extend(Group[] data, Group add) {
		Group[] r = new Group[data.length + 1];
		for (int i = 0; i < data.length; ++i) {
			r[i] = data[i];
		}
		r[data.length] = add;
		return r;
	}
	
	
	public static String[] take(String[] data, int n) {
		String[] r = new String[n];
		for (int i = 0; i < n; ++i) {
			r[i] = data[i];
		}
		return r;
	}
	
	public static boolean[] take(boolean[] data, int n) {
		boolean[] r = new boolean[n];
		for (int i = 0; i < n; ++i) {
			r[i] = data[i];
		}
		return r;
	}
	
	public static Double[] take(Double[] data, int n) {
		Double[] r = new Double[n];
		for (int i = 0; i < n; ++i) {
			r[i] = data[i];
		}
		return r;
	}
	
	public static Group[] take(Group[] data, int n) {
		Group[] r = new Group[n];
		for (int i = 0; i < n; ++i) {
			r[i] = data[i];
		}
		return r;
	}
	
	public static boolean safeCmp(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return true;
		} else if (o1 == null) {
			return false;
		} else if (o2 == null) {
			return false;
		} else {
			return o1.equals(o2);
		}
	}
}
