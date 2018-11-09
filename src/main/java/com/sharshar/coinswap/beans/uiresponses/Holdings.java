package com.sharshar.coinswap.beans.uiresponses;

import com.sharshar.coinswap.beans.OwnedAsset;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Created by lsharshar on 10/17/2018.
 */
@Data
@Accessors(chain = true)
public class Holdings {
	private double amountInBitcoin;
	private double amountInUSD;
	List<OwnedAssetUI> ownedAssets;
}
