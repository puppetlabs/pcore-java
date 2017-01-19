package com.puppet.pcore.impl;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Helpers {
	private static final Pattern CLASS_NAME = Pattern.compile("^[A-Z]\\w*(?:::[A-Z]\\w*)*$");
	private static final Pattern COLON_SPLIT = Pattern.compile("::");

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K,V> asMap(Object... keyValuePairs) {
		int len = keyValuePairs.length;
		switch(len) {
		case 0:
			return Collections.emptyMap();
		case 2:
			return Collections.singletonMap((K)keyValuePairs[0], (V)keyValuePairs[1]);
		default:
			if((len % 2) != 0)
				throw new IllegalArgumentException("asMap must be given an even number of arguments");
			Map<K,V> result = new LinkedHashMap<>();
			for(int idx = 0; idx < len; ) {
				Object key = keyValuePairs[idx++];
				result.put((K)key, (V)keyValuePairs[idx++]);
			}
			return result;
		}
	}

	public static String capitalizeSegment(String segment) {
		int len = segment.length();
		switch(len) {
		case 0:
			return segment;
		case 1:
			return segment.toUpperCase(Locale.ENGLISH);
		default:
			return new StringBuilder().append(Character.toUpperCase(segment.charAt(0))).append(segment, 1, len).toString();
		}
	}

	public static String capitalizeSegments(String typeName) {
		if(CLASS_NAME.matcher(typeName).matches())
			return typeName;
		String[] segments = COLON_SPLIT.split(typeName);
		if(segments.length == 1)
			return capitalizeSegment(segments[0]);
		StringJoiner joiner = new StringJoiner("::");
		for(String segment : segments)
			joiner.add(capitalizeSegment(segment));
		return joiner.toString();
	}

	@SuppressWarnings("unchecked")
	public static <T> T getArgument(String key, Map<String,Object> args, T defaultValue) {
		return args.containsKey(key) ? (T)args.get(key) : defaultValue;
	}

	public static String joinName(String[] segments) {
		return joinName(segments, 0);
	}

	public static String joinName(String[] segments, int startAt) {
		int nsegs = segments.length - startAt;
		if(nsegs <= 0)
			return "";

		if(nsegs == 1)
			return segments[startAt];

		StringBuilder bld = new StringBuilder(segments[startAt]);
		for(int idx = startAt + 1; idx < segments.length; ++idx) {
			bld.append("::");
			bld.append(segments[idx]);
		}
		return bld.toString();
	}

	public static <T> List<T> makeUnique(List<? extends T> a) {
		return new ArrayList<>(new LinkedHashSet<T>(a));
	}

	public static <T extends MergableRange<T>> List<T> mergeRanges(List<T> rangesToMerge) {
		switch(rangesToMerge.size()) {
		case 0:
		case 1:
			return rangesToMerge;
		default:
			List<T> result = new ArrayList<>();
			Stack<T> ranges = new Stack<>();
			ranges.addAll(rangesToMerge);
			while(!ranges.isEmpty()) {
				Stack<T> unmerged = new Stack<>();
				T x = ranges.pop();
				result.add(reduce(ranges, x, (memo, y) -> {
					T merged = memo.merge(y);
					if(merged == null)
						unmerged.add(y);
					else
						memo = merged;
					return memo;
				}));
				ranges = unmerged;
			}
			return result;
		}
	}

	public static <T> List<T> mergeUnique(List<? extends T> a, List<? extends T> b) {
		Set<T> uniqueSet = new LinkedHashSet<>(a);
		uniqueSet.addAll(b);
		return new ArrayList<>(uniqueSet);
	}

	public static void puppetQuote(String s, StringBuilder bld) {
		int top = s.length();
		boolean dQuote = false;
		int start = bld.length();
		bld.append('\'');
		for(int idx = 0; idx < top; ++idx) {
			char c = s.charAt(idx);
			switch(c) {
			case '\t':
				dQuote = true;
				bld.append("\\t");
				break;
			case '\n':
				dQuote = true;
				bld.append("\\n");
				break;
			case '\r':
				dQuote = true;
				bld.append("\\r");
				break;
			case '"':
				dQuote = true;
				bld.append("\\\"");
				break;
			case '\\':
				dQuote = true;
				bld.append("\\\\");
				break;
			case '\'':
			case '$':
				dQuote = true;
				bld.append(c);
				break;
			default:
				if(c < 0x20 || c > 0x7f) {
					dQuote = true;
					bld.append(String.format("\\u{%X}", (int)c));
				} else
					bld.append(c);
			}
		}
		if(dQuote) {
			bld.append('"');
			bld.setCharAt(start, '"');
		} else
			bld.append('\'');
	}

	public static void puppetRegexp(String s, StringBuilder bld) {
		int top = s.length();
		bld.append('/');
		for(int idx = 0; idx < top; ++idx) {
			char c = s.charAt(idx);
			switch(c) {
			case '\t':
				bld.append("\\t");
				break;
			case '\n':
				bld.append("\\n");
				break;
			case '\r':
				bld.append("\\r");
				break;
			case '\\':
				bld.append("\\\\");
				break;
			case '/':
				bld.append("\\/");
				break;
			default:
				if(c < 0x20 || c > 0x7f)
					bld.append(String.format("\\u{%X}", (int)c));
				else
					bld.append(c);
			}
		}
		bld.append('/');
	}

	public static String[] splitName(String qname) {
		return COLON_SPLIT.split(qname);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> unmodifiableCopy(List<? extends T> collection) {
		switch(collection.size()) {
		case 0:
			return Collections.emptyList();
		case 1:
			return Collections.unmodifiableList(Collections.singletonList(collection.get(0)));
		default:
			return Collections.unmodifiableList(Arrays.asList((T[])collection.toArray()));
		}
	}

	public static <T> List<T> unmodifiableCopy(T[] array) {
		return unmodifiableList(array.clone());
	}

	public static <T> List<T> unmodifiableList(T[] array) {
		switch(array.length) {
		case 0:
			return Collections.emptyList();
		case 1:
			return Collections.unmodifiableList(Collections.singletonList(array[0]));
		default:
			return Collections.unmodifiableList(Arrays.asList(array));
		}
	}

	public static <R> List<R> mapRange(int start, int end, Function<Integer, ? extends R> mapper) {
		List<R> result = new ArrayList<R>(end - start);
		for(int idx = start; idx < end; ++idx)
			result.add(mapper.apply(idx));
		return result;
	}

	public static <T> boolean all(Collection<? extends T> collection, Predicate<? super T> condition) {
		for(T elem : collection)
			if(!condition.test(elem))
				return false;
		return true;
	}

	public static <T> List<T> filter(Collection<? extends T> collection, Predicate<? super T> condition) {
		int top = collection.size();
		List<T> result = new ArrayList<T>(top);
		for(T elem : collection)
			if(condition.test(elem))
				result.add(elem);
		return result;
	}

	public static <T> int count(Collection<? extends T> collection, Predicate<? super T> condition) {
		int counter = 0;
		for(T elem : collection)
			if(condition.test(elem))
				++counter;
		return counter;
	}

	public static <T, R> List<R> map(Collection<? extends T> collection, Function<? super T, ? extends R> mapper) {
		int top = collection.size();
		List<R> result = new ArrayList<R>(top);
		for(T elem : collection)
			result.add(mapper.apply(elem));
		return result;
	}

	public static <T, R> R reduce(Collection<? extends T> collection, R identity, BiFunction<R, T, R> accumulator) {
		R result = identity;
		for(T elem : collection)
       result = accumulator.apply(result, elem);
     return result;
	}

	public static <T> boolean any(Collection<? extends T> collection, Predicate<? super T> condition) {
		for(T elem : collection)
			if(condition.test(elem))
				return true;
		return false;
	}

	public static <T> List<T> flatten(Collection<?> collection) {
		ArrayList<T> result = new ArrayList<>();
		for(Object elem : collection)
			if(elem instanceof Collection)
				result.addAll(flatten((Collection)elem));
			else
				result.add((T)elem);
		return result;
	}

	public static <T> List<T> distinct(Collection<? extends T> collection) {
		return new ArrayList<>(new LinkedHashSet<>(collection));
	}

	public static <T> Map<Boolean, List<T>> partitionBy(Collection<T> collection, Predicate<? super T> condition) {
		List<T> trueList = new ArrayList<>();
		List<T> falseList = new ArrayList<>();
		for(T elem : collection) {
			if(condition.test(elem))
				trueList.add(elem);
			else
				falseList.add(elem);
		}
		return asMap(true, trueList, false, falseList);
	}

	public static <T, G> Map<G, List<T>> groupBy(Collection<T> collection, Function<? super T, ? extends G> grouper) {
		Map<G, List<T>> result = new LinkedHashMap<G,List<T>>();
		for(T elem : collection) {
			G group = grouper.apply(elem);
			List<T> groupList = result.get(group);
			if(groupList == null) {
				groupList = new ArrayList<>();
				result.put(group, groupList);
			}
			groupList.add(elem);
		}
		return result;
	}
}
