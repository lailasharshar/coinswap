package com.sharshar.coinswap.utils;

import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Right now, just one service, should extend in the future
 *
 * Created by lsharshar on 7/20/2018.
 */
@Service
public class AccountServiceFinder {
	@Autowired
	private BinanceAccountServices binanceAccountServices;

	public AccountService getAccountService(ScratchConstants.Exchange exchange) {
		if (exchange == ScratchConstants.Exchange.BINANCE) {
			return binanceAccountServices;
		}
		return null;
	}
}
