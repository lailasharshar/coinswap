package com.sharshar.coinswap.repositories;

import com.sharshar.coinswap.beans.Swap;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by lsharshar on 7/20/2018.
 */
public interface SwapRepository extends CrudRepository<Swap, Long>{
	List<Swap> findByCoin1AndCoin2AndExchange(String coin1, String coin2, short exchange);
}
