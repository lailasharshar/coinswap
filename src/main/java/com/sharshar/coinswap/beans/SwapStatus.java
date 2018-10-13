package com.sharshar.coinswap.beans;

import com.sharshar.coinswap.utils.ScratchConstants;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Used to summarize the swaps and their order histories
 *
 * Created by lsharshar on 10/9/2018.
 */
@Data
@Accessors(chain = true)
public class SwapStatus {
	private Long id;
	private Boolean active;
	private Boolean simulate;
	private String coin1;
	private String coin2;
	private String baseCoin;
	private String commissionCoin;
	private boolean inSwap;
	private ScratchConstants.CurrentSwapState coinOwned;
	private List<OrderHistory> orderHistories;
}
