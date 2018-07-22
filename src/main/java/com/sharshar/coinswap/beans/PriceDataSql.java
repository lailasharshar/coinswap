package com.sharshar.coinswap.beans;


import javax.persistence.*;
import java.util.Date;

/**
 * Created by lsharshar on 5/14/2018.
 */
@Entity
@Table(name="pricedata")
public class PriceDataSql {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long tableId;
	private Double price;
	private Date updateTime;
	private String ticker;
	private Short exchange;

	public Long getTableId() {
		return tableId;
	}

	public PriceDataSql setTableId(Long tableId) {
		this.tableId = tableId;
		return this;
	}

	public Double getPrice() {
		return price;
	}

	public PriceDataSql setPrice(Double price) {
		this.price = price;
		return this;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public PriceDataSql setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
		return this;
	}

	public String getTicker() {
		return ticker;
	}

	public PriceDataSql setTicker(String ticker) {
		this.ticker = ticker;
		return this;
	}

	public Short getExchange() {
		return exchange;
	}

	public PriceDataSql setExchange(Short exchange) {
		this.exchange = exchange;
		return this;
	}
}
