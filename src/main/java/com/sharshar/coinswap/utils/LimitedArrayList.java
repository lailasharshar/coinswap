package com.sharshar.coinswap.utils;

import java.util.ArrayList;
import java.util.Collection;

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
	 * We want the old items to "fall" off the list.
	 *
	 * @param item - The item to add
	 */
	@Override
	public boolean add(T item) {
		boolean val = super.add(item);
		trimIt();
		return val;
	}

	@Override
	/**
	 * Use the super add all, but then trim it to the desired length
	 */
	public boolean addAll(Collection<? extends T> items) {
		boolean result = super.addAll(items);
		trimIt();
		return result;
	}

	/**
	 * Any time we may be longer than the default size, trim it
	 */
	private void trimIt() {
		while (this.size() > limit) {
			this.remove(0);
		}
	}
}
