package com.sharshar.coinswap.beans;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by lsharshar on 6/20/2018.
 */
@Entity
@Table(name="order_history")
public class OrderHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long tableId;

	private String clientOrderId;
	private long orderId;
	private String symbol;
	private String side;
	private long transactTime;
	private Date createDtm;
	private double amount;
	private String status;

	public long getTableId() {
		return tableId;
	}

	public OrderHistory setTableId(long tableId) {
		this.tableId = tableId;
		return this;
	}

	public String getClientOrderId() {
		return clientOrderId;
	}

	public OrderHistory setClientOrderId(String clientOrderId) {
		this.clientOrderId = clientOrderId;
		return this;
	}

	public long getOrderId() {
		return orderId;
	}

	public OrderHistory setOrderId(long orderId) {
		this.orderId = orderId;
		return this;
	}

	public String getSymbol() {
		return symbol;
	}

	public OrderHistory setSymbol(String symbol) {
		this.symbol = symbol;
		return this;
	}

	public String getSide() {
		return side;
	}

	public OrderHistory setSide(String side) {
		this.side = side;
		return this;
	}

	public long getTransactTime() {
		return transactTime;
	}

	public OrderHistory setTransactTime(long transactTime) {
		this.transactTime = transactTime;
		return this;
	}

	public Date getCreateDtm() {
		return createDtm;
	}

	public OrderHistory setCreateDtm(Date createDtm) {
		this.createDtm = createDtm;
		return this;
	}

	public double getAmount() {
		return amount;
	}

	public OrderHistory setAmount(double amount) {
		this.amount = amount;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public OrderHistory setStatus(String status) {
		this.status = status;
		return this;
	}
}
