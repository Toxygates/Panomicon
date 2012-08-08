package otgviewer.shared;

public class SharedUtils {
	
	public static <T> int indexOf(T[] haystack, T needle) {
		for (int i = 0; i < haystack.length; ++i) {
			if (haystack[i].equals(needle)) {
				return i;
			}
		}
		return -1;
	}

}
