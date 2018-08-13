package com.sharshar.coinswap.beans.simulation;

import com.sharshar.coinswap.components.SwapExecutor;

import java.text.SimpleDateFormat;
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
	private SwapExecutor.ResponseCode responseCode;

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

	public SwapExecutor.ResponseCode getResponseCode() {
		return responseCode;
	}

	public TradeAction setResponseCode(SwapExecutor.ResponseCode responseCode) {
		this.responseCode = responseCode;
		return this;
	}

	@Override
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		String dateVal = tradeDate.toString();
		try {
			dateVal = sdf.format(tradeDate);
		} catch (Exception ex) {}
		StringBuilder s = new StringBuilder();
		s.append("Trade: ").append(dateVal).append(": C1 - ").append(String.format("%.4f", amountCoin1))
				.append(", C2 - ").append(String.format("%.4f", amountCoin2))
				.append(", ").append(direction);
		return s.toString();
	}
}

