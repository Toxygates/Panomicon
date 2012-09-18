package otgviewer.shared;

import java.util.List;

import scala.actors.threadpool.Arrays;

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
	
	public static DataColumn unpackColumn(String s) {		
		String[] spl = s.split("^^^");
		if (spl[0].equals("Barcode")) {
			return Barcode.unpack(s);
		} else {
			return Group.unpack(s);
		}
	}
	
}
