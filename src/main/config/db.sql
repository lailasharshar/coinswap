DROP TABLE IF EXISTS `order_history`;
CREATE TABLE `order_history` (
  `table_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `client_order_id` varchar(255) NOT NULL,
  `order_id` bigint(20) NOT NULL,
  `symbol` varchar(45) NOT NULL,
  `side` varchar(45) DEFAULT NULL,
  `transact_time` bigint(20) DEFAULT NULL,
  `create_dtm` datetime DEFAULT NULL,
  `amount` double DEFAULT NULL,
  `status` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `simulation_run`;
CREATE TABLE `simulation_run` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `simulation_start_time` datetime DEFAULT NULL,
  `simulation_end_time` datetime DEFAULT NULL,
  `coin1` varchar(25) DEFAULT NULL,
  `coin2` varchar(25) DEFAULT NULL,
  `base_coin` varchar(25) DEFAULT NULL,
  `std_dev` double DEFAULT NULL,
  `commission_coin` varchar(25) DEFAULT NULL,
  `snapshot_interval` bigint(20) DEFAULT NULL,
  `start_amount` double DEFAULT NULL,
  `end_amount` double DEFAULT NULL,
  `mean_change` double DEFAULT NULL,
  `std_dev_change` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `swap`;
CREATE TABLE `swap` (
  `table_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `coin1` varchar(20) NOT NULL,
  `coin2` varchar(20) NOT NULL,
  `exchange` tinyint(4) NOT NULL,
  `base_coin` varchar(20) DEFAULT NULL,
  `commission_coin` varchar(20) NOT NULL,
  `active` tinyint(4) DEFAULT NULL,
  `desired_std_dev` double DEFAULT NULL,
  `simulate` tinyint(4) DEFAULT NULL,
  `max_percent_volume` double DEFAULT NULL,
  `last_volume1` double DEFAULT NULL,
  `last_volume2` double DEFAULT NULL,
  `coin_owned` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `tickers`;
CREATE TABLE `tickers` (
  `table_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ticker` varchar(45) DEFAULT NULL,
  `base` varchar(45) DEFAULT NULL,
  `found_date` datetime DEFAULT NULL,
  `exchange` tinyint(4) DEFAULT NULL,
  `min_qty` double DEFAULT NULL,
  `max_qty` varchar(45) DEFAULT NULL,
  `step_size` double DEFAULT NULL,
  `retired` datetime DEFAULT NULL,
  `updated_date` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `trade_action`;
CREATE TABLE `trade_action` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `simulation_id` bigint(20) NOT NULL,
  `trade_date` datetime DEFAULT NULL,
  `direction` varchar(45) DEFAULT NULL,
  `amount_coin1` double DEFAULT NULL,
  `amount_coin2` double DEFAULT NULL,
  `price_coin1` double DEFAULT NULL,
  `price_coin2` double DEFAULT NULL,
  `response_code` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `simulation_id_fk_idx` (`simulation_id`),
  CONSTRAINT `simulation_id_fk` FOREIGN KEY (`simulation_id`) REFERENCES `simulation_run` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
