package com.sharshar.coinswap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Used in testing to turn off scheduling in the test so we don't have unintended consequences
 * Created by lsharshar on 7/29/2018.
 */
@SpringBootApplication
public class TestCoinswapApplication {
	public static void main(String[] args) {
		SpringApplication.run(TestCoinswapApplication.class, args);
	}
}
