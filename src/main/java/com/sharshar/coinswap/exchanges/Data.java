package com.sharshar.coinswap.exchanges;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * Holds that data queried from historical data
 *
 * Created by lsharshar on 5/11/2018.
 */
@JsonIgnoreProperties
public class Data {
	private long time;
	private double high;
	private double low;
	private double open;
	private double volumefrom;
	private double volumeto;
	private double close;

	public Date getTime() {
		return new Date(time * 1000);
	}

	public void setTime(long time) {
		this.time = time;
	}

	public double getHigh() {
		return high;
	}

	public void setHigh(double high) {
		this.high = high;
	}

	public double getLow() {
		return low;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public double getOpen() {
		return open;
	}

	public void setOpen(double open) {
		this.open = open;
	}

	public double getVolumefrom() {
		return volumefrom;
	}

	public void setVolumefrom(double volumefrom) {
		this.volumefrom = volumefrom;
	}

	public double getVolumeto() {
		return volumeto;
	}

	public void setVolumeto(double volumeto) {
		this.volumeto = volumeto;
	}

	public double getClose() {
		return close;
	}

	public void setClose(double close) {
		this.close = close;
	}

	public String toString() {
		String format = "%.5f";
		StringBuilder builder = new StringBuilder();
		builder.append(getTime().toString())
				.append(" OHLC Average: ").append(String.format(format, getOhlcAverage()))
				.append(" Range: ").append(String.format(format, getLow())).append("-").append(String.format(format, getHigh()))
				.append(" Open/Close: ").append(String.format(format, getOpen())).append("-").append(String.format(format, getClose()));
		return builder.toString();
	}

	public double getOhlcAverage() {
		return (getHigh() + getLow() + getOpen() + getClose())/4;
	}
}
