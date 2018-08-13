package com.sharshar.coinswap.utils;

import java.util.ArrayList;

/**
 * Used to create a first in first out limited size list
 *
 * Created by lsharshar on 3/21/2018.
 */
public class LimitedArrayList<T> extends ArrayList<T> {
	private int limit;

	public LimitedArrayList(int limit) {
		this.limit = limit;
	}

	/**
	 * If we have more than limit number of items, remove the first and then add the new item.
	 * We want the old items to "fall" off the list. Not sure what addAll will do, so stick to this for now
	 *
	 * @param item - The item to add
	 */
	@Override
	public boolean add(T item) {
		if (this.size() >= limit) {
			this.remove(0);
		}
		return super.add(item);
	}
}
