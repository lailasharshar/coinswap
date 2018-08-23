package com.sharshar.coinswap.beans;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * An object to hold the balance of a particular currency
 *
 * Created by lsharshar on 5/27/2018.
 */
@Data
@Accessors(chain = true)
public class OwnedAsset {
	private String asset;
	private double free;
	private double locked;
}
