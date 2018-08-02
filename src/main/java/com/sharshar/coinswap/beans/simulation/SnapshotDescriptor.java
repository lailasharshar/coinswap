package com.sharshar.coinswap.beans.simulation;

import com.sharshar.coinswap.beans.PriceData;

import java.util.Date;

/**
 * After a simulation is performed, this is snapshot information for a defined period of this. This allows us
 * determine if there is a regular progression across time as opposed to a lucky, sudden surge.
 *
 * Created by lsharshar on 7/31/2018.
 */
public class SnapshotDescriptor {
	private Date snapshotDate;
	private double amountCoin1;
	private double amountCoin2;
	private double amountCommissionCoin;
	private PriceData coinPd1;
	private PriceData coinPd2;
	private PriceData commissionCoin;

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		double baseAmt1 = amountCoin1 * coinPd1.getPrice();
		double baseAmt2 = amountCoin2 * coinPd2.getPrice();
		double baseAmtCommission = amountCommissionCoin * commissionCoin.getPrice();
		s.append("Snapshot - ").append(snapshotDate)
				.append(": C1 ").append(amountCoin1).append(" (").append(String.format("%.6f", coinPd1.getPrice())).append(" = ").append(String.format("%.6f", baseAmt1))
				.append("), C2 ").append(amountCoin2).append(" (").append(String.format("%.6f", coinPd2.getPrice())).append(" = ").append(String.format("%.6f", baseAmt2))
				.append("), CCoin ").append(amountCommissionCoin).append(" (").append(String.format("%.6f", commissionCoin.getPrice())).append(") = ").append(String.format("%.6f", baseAmtCommission))
				.append(", Total: ").append(String.format("%.6f", baseAmt1 + baseAmt2 + baseAmtCommission));
		return s.toString();
	}

	public double getTotalValue() {
		double baseAmt1 = amountCoin1 * coinPd1.getPrice();
		double baseAmt2 = amountCoin2 * coinPd2.getPrice();
		double baseAmtCommission = amountCommissionCoin * commissionCoin.getPrice();
		return baseAmt1 + baseAmt2 + baseAmtCommission;
	}

	public Date getSnapshotDate() {
		return snapshotDate;
	}

	public SnapshotDescriptor setSnapshotDate(Date snapshotDate) {
		this.snapshotDate = snapshotDate;
		return this;
	}

	public double getAmountCoin1() {
		return amountCoin1;
	}

	public SnapshotDescriptor setAmountCoin1(double amountCoin1) {
		this.amountCoin1 = amountCoin1;
		return this;
	}

	public double getAmountCoin2() {
		return amountCoin2;
	}

	public SnapshotDescriptor setAmountCoin2(double amountCoin2) {
		this.amountCoin2 = amountCoin2;
		return this;
	}

	public double getAmountCommissionCoin() {
		return amountCommissionCoin;
	}

	public SnapshotDescriptor setAmountCommissionCoin(double amountCommissionCoin) {
		this.amountCommissionCoin = amountCommissionCoin;
		return this;
	}

	public PriceData getCoinPd1() {
		return coinPd1;
	}

	public SnapshotDescriptor setCoinPd1(PriceData coinPd1) {
		this.coinPd1 = coinPd1;
		return this;
	}

	public PriceData getCoinPd2() {
		return coinPd2;
	}

	public SnapshotDescriptor setCoinPd2(PriceData coinPd2) {
		this.coinPd2 = coinPd2;
		return this;
	}

	public PriceData getCommissionCoin() {
		return commissionCoin;
	}

	public SnapshotDescriptor setCommissionCoin(PriceData commissionCoin) {
		this.commissionCoin = commissionCoin;
		return this;
	}

}
