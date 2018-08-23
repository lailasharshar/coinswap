package com.sharshar.coinswap.beans;

import com.sharshar.coinswap.utils.ScratchConstants;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * Bean for price data
 *
 * Created by lsharshar on 3/6/2018.
 */
@Data
@Accessors(chain = true)
public class PriceData {

	private String ticker;
	private Double price;
	private Date updateTime;
	private ScratchConstants.Exchange exchange;
}
