package com.sharshar.coinswap.utils;

import java.util.stream.Stream;

/**
 * Reused constants for the application that don't need to be in a configuration file
 *
 * Created by lsharshar on 3/19/2018.
 */
public class ScratchConstants {
	private ScratchConstants() {  /* No need to instantiate this */}

	public enum Exchange {
		BINANCE((short) 1, "Binance"), CRYPTO_COMPARE((short) 2, "Crypto Compare");

		private short value = 0;
		private String exchangeName;

		public String getExchangeName() {
			return this.exchangeName;
		}

		public short getValue() {
			return value;
		}

		public static Exchange valueOf(int value) {
			return Stream.of(Exchange.values()).filter(c -> c.getValue() == value).findFirst().orElse(null);
		}

		Exchange(short value, String exchangeName) {
			this.value = value;
			this.exchangeName = exchangeName;
		}
	}

	// The maximum amount of time this api can be down
	// before the data is stale is 30 second
	public static final long MAX_EXCHANGE_DOWN_TIME = 1000L * 30;

	// The maximum amount of time this service can be down
	// before the data in the database can't be trusted
	public static final long MAX_TIME_BEFORE_RELOAD_REQUIRED =  1000L * 30;

	public static final long ONE_DAY = 1000L * 60 * 60 * 24;

	public enum CurrentSwapState {
		OWNS_COIN_1,
		OWNS_COIN_2,
		OWNS_NOTHING
	}
}
