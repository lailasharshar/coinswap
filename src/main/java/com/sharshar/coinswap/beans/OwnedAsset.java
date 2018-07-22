package com.sharshar.coinswap.beans;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * An object to hold the balance of a particular currency
 *
 * Created by lsharshar on 5/27/2018.
 */
public class OwnedAsset {
	private String asset;
	private double free;
	private double locked;

	public String getAsset() {
		return asset;
	}

	public OwnedAsset setAsset(String asset) {
		this.asset = asset;
		return this;
	}

	public double getFree() {
		return free;
	}

	public OwnedAsset setFree(double free) {
		this.free = free;
		return this;
	}

	public double getLocked() {
		return locked;
	}

	public OwnedAsset setLocked(double locked) {
		this.locked = locked;
		return this;
	}

	@Override
	public String toString() {
		return (new ToStringBuilder(this,
				ToStringStyle.SHORT_PREFIX_STYLE))
				.append("asset", this.asset)
				.append("free", this.free)
				.append("locked", this.locked).toString();
	}
}
