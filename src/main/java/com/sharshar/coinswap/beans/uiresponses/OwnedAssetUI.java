package com.sharshar.coinswap.beans.uiresponses;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Summarized version of owned asset so it is easier to figure out asset allocation
 *
 * Created by lsharshar on 10/17/2018.
 */
@Data
@Accessors(chain = true)
public class OwnedAssetUI {
	private String asset;
	private double total;
	private double price;
	private double inBTC;
	private double inUSD;
	private double percentOfPortfolio;
}
