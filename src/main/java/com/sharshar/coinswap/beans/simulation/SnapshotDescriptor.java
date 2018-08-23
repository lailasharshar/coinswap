package com.sharshar.coinswap.beans.simulation;

import com.sharshar.coinswap.beans.PriceData;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * After a simulation is performed, this is snapshot information for a defined period of this. This allows us
 * determine if there is a regular progression across time as opposed to a lucky, sudden surge.
 *
 * Created by lsharshar on 7/31/2018.
 */
@Data
@Accessors(chain = true)
public class SnapshotDescriptor {
	private Date snapshotDate;
	private double amountCoin1;
	private double amountCoin2;
	private double amountCommissionCoin;
	private PriceData coinPd1;
	private PriceData coinPd2;
	private PriceData commissionCoin;

	public double getTotalValue() {
		double baseAmt1 = amountCoin1 * coinPd1.getPrice();
		double baseAmt2 = amountCoin2 * coinPd2.getPrice();
		double baseAmtCommission = amountCommissionCoin * commissionCoin.getPrice();
		return baseAmt1 + baseAmt2 + baseAmtCommission;
	}
}
