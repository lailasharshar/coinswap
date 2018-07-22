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

	public Ticker() {
		this.foundDate = new Date();
	}

	public Ticker(String tickerAndBase, short exchange, List<String> baseCurrencies) {
		this.exchange = exchange;
		this.foundDate = new Date();
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
}
