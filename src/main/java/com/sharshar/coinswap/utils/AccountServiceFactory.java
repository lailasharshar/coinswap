package com.sharshar.coinswap.utils;

import com.sharshar.coinswap.exchanges.AccountService;
import com.sharshar.coinswap.exchanges.binance.BinanceAccountServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by lsharshar on 7/20/2018.
 */
@Service
public class AccountServiceFactory {
	@Autowired
	private BinanceAccountServices binanceAccountServices;

	public AccountService getAccountService(short id) {
		if (id == ScratchConstants.BINANCE) {
			return binanceAccountServices;
		}
		return null;
	}
}
