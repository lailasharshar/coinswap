package com.sharshar.coinswap.services;

import com.sharshar.coinswap.TestCoinswapApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Tests setting the different statuses of the system
 *
 * Created by lsharshar on 8/26/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestCoinswapApplication.class)
public class MasterSettingsServiceTest {
	@Autowired
	private MasterSettingsService masterSettingsService;

	@Test
	public void testStatus() throws Exception {
		assertTrue(masterSettingsService.areWeRunning());
		assertEquals(masterSettingsService.getStatus(), MasterSettingsService.SystemStatus.RUNNING);
		masterSettingsService.pauseService();
		assertEquals(masterSettingsService.getStatus(), MasterSettingsService.SystemStatus.PAUSED);
		assertFalse(masterSettingsService.areWeRunning());
		masterSettingsService.resumeService();
		assertEquals(masterSettingsService.getStatus(), MasterSettingsService.SystemStatus.RUNNING);
		assertTrue(masterSettingsService.areWeRunning());
		masterSettingsService.shutdown();
		assertEquals(masterSettingsService.getStatus(), MasterSettingsService.SystemStatus.DISABLED);
		assertFalse(masterSettingsService.areWeRunning());
		masterSettingsService.resumeService();
		assertFalse(masterSettingsService.areWeRunning());
	}
}