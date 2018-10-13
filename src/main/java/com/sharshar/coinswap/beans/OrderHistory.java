package com.sharshar.coinswap.beans;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

/**
 * Describes an order
 *
 * Created by lsharshar on 6/20/2018.
 */
@Entity
@Table(name="order_history")
@Data
@Accessors(chain = true)
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
	private Double price;
	private Long swapId;
}
