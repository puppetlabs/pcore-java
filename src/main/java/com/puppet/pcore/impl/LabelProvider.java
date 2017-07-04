package com.puppet.pcore.impl;

public class LabelProvider {
	static final char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u', 'y', 'A', 'E', 'I', 'O', 'U', 'Y'};

	/**
	 * Produces a label for the given object with indefinite article (a/an)
	 *
	 * @param o the object to produce a label for
	 * @return the label
	 */
	public static String aOrAn(Object o) {
		String txt = label(o);
		return article(txt, false) + ' ' + txt;
	}

	/**
	 * Produces a label for the given object with indefinite article (A/An)
	 *
	 * @param o the object to produce a label for
	 * @return the label
	 */
	public static String aOrAnUc(Object o) {
		String txt = label(o);
		return article(txt, true) + ' ' + txt;
	}

	public static String article(String txt) {
		return article(txt, false);
	}

	public static String article(String txt, boolean uc) {
		if(txt.length() > 0) {
			char c = txt.charAt(0);
			for(char w : vowels)
				if(c == w)
					return uc ? "An" : "an";
			return uc ? "A" : "a";
		}
		return "";
	}

	/**
	 * Provides a label for the given object by calling {@link #toString()} on the object.
	 *
	 * @param o the object to produce a label for
	 * @return the label
	 */
	public static String label(Object o) {
		return o.toString();
	}
}
