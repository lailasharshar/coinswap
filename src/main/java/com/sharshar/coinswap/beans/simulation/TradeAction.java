package com.sharshar.coinswap.beans.simulation;

import java.util.Date;

/**
 * Describes a simulated trade
 *
 * Created by lsharshar on 7/31/2018.
 */
public class TradeAction {
	private Date tradeDate;
	private SimulatorRecord.TradeDirection direction;
	private double amountCoin1;
	private double amountCoin2;

	public Date getTradeDate() {
		return tradeDate;
	}

	public TradeAction setTradeDate(Date tradeDate) {
		this.tradeDate = tradeDate;
		return this;
	}

	public SimulatorRecord.TradeDirection getDirection() {
		return direction;
	}

	public TradeAction setDirection(SimulatorRecord.TradeDirection direction) {
		this.direction = direction;
		return this;
	}

	public double getAmountCoin1() {
		return amountCoin1;
	}

	public TradeAction setAmountCoin1(double amountCoin1) {
		this.amountCoin1 = amountCoin1;
		return this;
	}

	public double getAmountCoin2() {
		return amountCoin2;
	}

	public TradeAction setAmountCoin2(double amountCoin2) {
		this.amountCoin2 = amountCoin2;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("Trade: ").append(tradeDate).append(": C1 ").append(String.format("%.6f", amountCoin1))
				.append(", C2 ").append(String.format("%.6f", amountCoin2))
				.append(", ").append(direction);
		return s.toString();
	}
}

