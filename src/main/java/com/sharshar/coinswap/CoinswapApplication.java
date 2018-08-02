package com.sharshar.coinswap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoinswapApplication {
	public static void main(String[] args) {
		SpringApplication.run(CoinswapApplication.class, args);
	}
}
