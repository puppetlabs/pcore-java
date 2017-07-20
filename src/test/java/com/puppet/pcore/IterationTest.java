package com.puppet.pcore;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class IterationTest {
	static Integer[] createArray() {

		Integer sArray[] = new Integer[5];

		for (int i = 0; i < 5; i++)
			sArray[i] = i;

		return sArray;
	}

	static int testIterator(List<Integer> lList, int sum) {
		// iterator loop
		Iterator<Integer> iterator = lList.iterator();
		while(iterator.hasNext()) {
			sum += iterator.next();
		}
		return sum;
	}

	static int testIterable(List<Integer> lList, int sum) {
		// iterator loop
		for(Integer stemp : lList)
			sum += stemp;
		return sum;
	}

	static int testFor(List<Integer> lList, int sum) {
		final int t = lList.size();
		for(int i = 0; i < t; ++i)
			sum += lList.get(i);
		return sum;
	}

	static int testFor2(List<Integer> lList, int sum) {
		final int t = lList.size();
		int s[] = new int[] { sum };
		for(int i = 0; i < t; ++i)
			s[0] += lList.get(i);
		return s[0];
	}

	static int testForWithBlock(List<Integer> lList, int sum) {
		final int t = lList.size();
		int s[] = new int[] { sum };
		Consumer<Integer> func = new Consumer<Integer>() {
			@Override
			public void accept(Integer integer) {
				s[0] += integer;
			}
		};

		for(int i = 0; i < t; ++i)
			func.accept(lList.get(i));
		return s[0];
	}

	static int testForEach(List<Integer> lList, int sum) {
		int s[] = new int[] { sum };
		lList.forEach(n -> s[0] += n);
		return s[0];
	}

	static int testWhile(List<Integer> lList, int sum) {
		// iterator loop
		int j = 0;
		while(j < lList.size())
			sum += lList.get(j++);
		return sum;
	}

	static int testWhile2(List<Integer> lList, int sum) {
		// iterator loop
		int j = lList.size();
		while(--j >= 0)
			sum += lList.get(j);
		return sum;
	}

	// @Test
	void doIt() {
		Integer sArray[] = createArray();

		// convert array to list
		List<Integer> lList = Arrays.asList(sArray);
		final int tests = 1000000000;

		System.out.println("\n--------- Iterator Loop -------\n");
		long startTime = new Date().getTime();
		System.out.println("Start: " + startTime);
		int sum = 0;
		for(int tn = 0; tn < tests; ++tn)
			sum = testIterator(lList, sum);
		long endTime = new Date().getTime();
		System.out.println("End: " + endTime + ' ' + sum);
		System.out.println("Iterator - Elapsed time in milliseconds: " + (endTime - startTime));

		System.out.println("\n--------- Iterable Loop -------\n");
		startTime = new Date().getTime();
		System.out.println("Start: " + startTime);
		sum = 0;
		for(int tn = 0; tn < tests; ++tn)
			sum = testIterable(lList, sum);
		endTime = new Date().getTime();
		System.out.println("End: " + endTime + ' ' + sum);
		System.out.println("Iteratable - Elapsed time in milliseconds: " + (endTime - startTime));

		System.out.println("\n--------- For Loop --------\n");
		startTime = new Date().getTime();
		System.out.println("Start: " + startTime);
		sum = 0;
		for(int tn = 0; tn < tests; ++tn)
			sum = testFor(lList, sum);
		endTime = new Date().getTime();
		System.out.println("End: " + endTime + ' ' + sum);
		System.out.println("For - Elapsed time in milliseconds: " + (endTime - startTime));

		System.out.println("\n--------- For 2 Loop --------\n");
		startTime = new Date().getTime();
		System.out.println("Start: " + startTime);
		sum = 0;
		for(int tn = 0; tn < tests; ++tn)
			sum = testFor2(lList, sum);
		endTime = new Date().getTime();
		System.out.println("End: " + endTime + ' ' + sum);
		System.out.println("For 2 - Elapsed time in milliseconds: " + (endTime - startTime));

		System.out.println("\n--------- For with block Loop --------\n");
		startTime = new Date().getTime();
		System.out.println("Start: " + startTime);
		sum = 0;
		for(int tn = 0; tn < tests; ++tn)
			sum = testForWithBlock(lList, sum);
		endTime = new Date().getTime();
		System.out.println("End: " + endTime + ' ' + sum);
		System.out.println("For 2 - Elapsed time in milliseconds: " + (endTime - startTime));

		System.out.println("\n--------- Foreach Loop --------\n");
		startTime = new Date().getTime();
		System.out.println("Start: " + startTime);
		sum = 0;
		for(int tn = 0; tn < tests; ++tn)
			sum = testForEach(lList, sum);
		endTime = new Date().getTime();
		System.out.println("End: " + endTime + ' ' + sum);
		System.out.println("For 2 - Elapsed time in milliseconds: " + (endTime - startTime));

		System.out.println("\n--------- While Loop -------\n");
		startTime = new Date().getTime();
		System.out.println("Start: " + startTime);
		sum = 0;
		for(int tn = 0; tn < tests; ++tn)
			sum = testWhile(lList, sum);
		endTime = new Date().getTime();
		System.out.println("End: " + endTime + ' ' + sum);
		System.out.println("While - Elapsed time in milliseconds: " + (endTime - startTime));

		System.out.println("\n--------- While Loop 2 -------\n");
		startTime = new Date().getTime();
		System.out.println("Start: " + startTime);
		sum = 0;
		for(int tn = 0; tn < tests; ++tn)
			sum = testWhile2(lList, sum);
		endTime = new Date().getTime();
		System.out.println("End: " + endTime + ' ' + sum);
		System.out.println("While 2 - Elapsed time in milliseconds: " + (endTime - startTime));
	}
}
