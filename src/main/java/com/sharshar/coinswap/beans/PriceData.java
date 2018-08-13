package com.sharshar.coinswap.beans;

import com.sharshar.coinswap.utils.ScratchConstants;

import java.util.Date;

/**
 * Bean for price data
 *
 * Created by lsharshar on 3/6/2018.
 */
public class PriceData {

	private String ticker;
	private Double price;
	private Date updateTime;
	private ScratchConstants.Exchange exchange;

	public Double getPrice() {
		return price;
	}

	public PriceData setPrice(Double price) {
		this.price = price;
		return this;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public PriceData setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
		return this;
	}

	public String getTicker() {
		return ticker;
	}

	public PriceData setTicker(String ticker) {
		this.ticker = ticker;
		return this;
	}

	public ScratchConstants.Exchange getExchange() {
		return exchange;
	}

	public PriceData setExchange(ScratchConstants.Exchange exchange) {
		this.exchange = exchange;
		return this;
	}

	@Override
	public String toString() {
		return ticker + " = " + price + " on " + exchange.getExchangeName() + " (" + updateTime + ")";
	}
}
