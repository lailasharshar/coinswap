package com.sharshar.coinswap.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test our limited array
 *
 * Created by lsharshar on 8/23/2018.
 */
public class LimitedArrayListTest {
	@Test
	public void testArray() throws Exception {
		List<Integer> list = new LimitedArrayList<>(5);
		assertEquals(0, list.size());
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);
		list.add(5);
		assertEquals(5, list.size());
		assertEquals(1, list.get(0).intValue());
		assertEquals(5, list.get(4).intValue());
		list.add(6);
		assertEquals(2, list.get(0).intValue());
		assertEquals(6, list.get(4).intValue());

		List<Integer> list2 = new ArrayList<>();
		list2.add(7);
		list2.add(8);

		list.addAll(list2);
		assertEquals(5, list.size());
		assertEquals(4, list.get(0).intValue());
		assertEquals(8, list.get(4).intValue());
	}

}