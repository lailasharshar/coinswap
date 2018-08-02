package com.sharshar.coinswap.beans;

import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Describes all coins in a particular exchange. Allows us to determine when they are added (or first time db is
 * populated)
 *
 * Created by lsharshar on 7/16/2018.
 */
@Entity
@Table(name="tickers")
public class Ticker {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	long tableId;
	private String ticker;
	private String base;
	private short exchange;
	private Date foundDate;
	private Date retired;
	private Date updatedDate;
	private Double minQty;
	private Double maxQty;
	private Double stepSize;
	private Double lastVolume;

	public Ticker() {
	}

	public Double getLastVolume() {
		return lastVolume;
	}

	public Ticker setLastVolume(Double lastVolume) {
		this.lastVolume = lastVolume;
		return this;
	}

	public Ticker(String tickerAndBase, short exchange, List<String> baseCurrencies) {
		this.exchange = exchange;
		if (baseCurrencies == null || tickerAndBase == null || tickerAndBase.isEmpty()) {
			return;
		}
		String foundBase = "";
		for (String base : baseCurrencies) {
			if (tickerAndBase.endsWith(base)) {
				if (base.length() > foundBase.length()) {
					foundBase = base;
				}
			}
		}
		if (foundBase.length() > 0) {
			this.ticker = tickerAndBase.replaceAll(foundBase, "");
			this.base = foundBase;
		}
	}

	public long getTableId() {
		return tableId;
	}

	public Ticker setTableId(long tableId) {
		this.tableId = tableId;
		return this;
	}

	public String getTicker() {
		return ticker;
	}

	public Ticker setTicker(String ticker) {
		this.ticker = ticker;
		return this;
	}

	public String getBase() {
		return base;
	}

	public Ticker setBase(String base) {
		this.base = base;
		return this;
	}

	public Date getFoundDate() {
		return foundDate;
	}

	public Ticker setFoundDate(Date foundDate) {
		this.foundDate = foundDate;
		return this;
	}

	public short getExchange() {
		return exchange;
	}

	public Ticker setExchange(short exchange) {
		this.exchange = exchange;
		return this;
	}

	public Date getRetired() {
		return retired;
	}

	public Ticker setRetired(Date retired) {
		this.retired = retired;
		return this;
	}

	public double getMinQty() {
		return minQty;
	}

	public Ticker setMinQty(double minQty) {
		this.minQty = minQty;
		return this;
	}

	public double getMaxQty() {
		return maxQty;
	}

	public Ticker setMaxQty(double maxQty) {
		this.maxQty = maxQty;
		return this;
	}

	public double getStepSize() {
		return stepSize;
	}

	public Ticker setStepSize(double stepSize) {
		this.stepSize = stepSize;
		return this;
	}

	public String getTickerBase() {
		return ticker + base;
	}

	public Date getUpdatedDate() {
		return updatedDate;
	}

	public Ticker setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
		return this;
	}

	public Ticker setMinQty(Double minQty) {
		this.minQty = minQty;
		return this;
	}

	public Ticker setMaxQty(Double maxQty) {
		this.maxQty = maxQty;
		return this;
	}

	public Ticker setStepSize(Double stepSize) {
		this.stepSize = stepSize;
		return this;
	}
}
