package com.sharshar.coinswap.beans.simulation;

import com.sharshar.coinswap.beans.PriceData;
import com.sharshar.coinswap.beans.SwapDescriptor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Holds summary information for analysis
 * Created by lsharshar on 7/30/2018.
 */
public class SimulatorRecord {
	public enum TradeDirection {
		BUY_COIN_2,
		BUY_COIN_1
	}

	private double initialBaseInvestment;
	private SwapDescriptor descriptor;
	private List<SnapshotDescriptor> snapshotDescriptorList;
	private double desiredStdDev;
	private List<TradeAction> tradeActionList;
	private Date startDate;
	private Date endDate;

	public void addSnapshot(Date snapshotDate, double amountCoin1, double amountCoin2, double amountCommissionCoin,
			PriceData coinPd1, PriceData coinPd2, PriceData commissionCoin) {
		if (snapshotDescriptorList == null) {
			snapshotDescriptorList = new ArrayList<>();
		}
		SnapshotDescriptor sd = new SnapshotDescriptor();
		sd.setSnapshotDate(snapshotDate).setAmountCoin1(amountCoin1).setAmountCoin2(amountCoin2)
				.setAmountCommissionCoin(amountCommissionCoin).setCoinPd1(coinPd1).setCoinPd2(coinPd2)
				.setCommissionCoin(commissionCoin);
		getSnapshotDescriptorList().add(sd);
	}
	public double getDesiredStdDev() {
		return desiredStdDev;
	}

	public SimulatorRecord setDesiredStdDev(double desiredStdDev) {
		this.desiredStdDev = desiredStdDev;
		return this;
	}

	public double getInitialBaseInvestment() {
		return initialBaseInvestment;
	}

	public SimulatorRecord setInitialBaseInvestment(double initialBaseInvestment) {
		this.initialBaseInvestment = initialBaseInvestment;
		return this;
	}

	public SwapDescriptor getDescriptor() {
		return descriptor;
	}

	public SimulatorRecord setDescriptor(SwapDescriptor descriptor) {
		this.descriptor = descriptor;
		return this;
	}

	public List<SnapshotDescriptor> getSnapshotDescriptorList() {
		if (snapshotDescriptorList == null) {
			snapshotDescriptorList = new ArrayList<>();
		}
		return snapshotDescriptorList;
	}

	public SimulatorRecord setSnapshotDescriptorList(List<SnapshotDescriptor> snapshotDescriptorList) {
		this.snapshotDescriptorList = snapshotDescriptorList;
		return this;
	}

	public List<TradeAction> getTradeActionList() {
		if (tradeActionList == null) {
			tradeActionList = new ArrayList<>();
		}
		return tradeActionList;
	}

	public void addTradeAction(Date tradeDate, TradeDirection direction, double amountCoin1, double amountCoin2) {
		TradeAction ta = new TradeAction().setTradeDate(tradeDate).setDirection(direction).setAmountCoin1(amountCoin1)
				.setAmountCoin2(amountCoin2);
		getTradeActionList().add(ta);
	}

	public SimulatorRecord setTradeActionList(List<TradeAction> tradeActionList) {
		this.tradeActionList = tradeActionList;
		return this;
	}

	public Date getStartDate() {
		return startDate;
	}

	public SimulatorRecord setStartDate(Date startDate) {
		this.startDate = startDate;
		return this;
	}

	public Date getEndDate() {
		return endDate;
	}

	public SimulatorRecord setEndDate(Date endDate) {
		this.endDate = endDate;
		return this;
	}
}
