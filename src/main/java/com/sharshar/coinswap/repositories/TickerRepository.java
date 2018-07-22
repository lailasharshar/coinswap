package com.sharshar.coinswap.repositories;

import com.sharshar.coinswap.beans.Ticker;
import org.springframework.data.repository.CrudRepository;

/**
 * Access the list of available tickers
 *
 * Created by lsharshar on 7/16/2018.
 */
public interface TickerRepository extends CrudRepository<Ticker, Long>{
}
