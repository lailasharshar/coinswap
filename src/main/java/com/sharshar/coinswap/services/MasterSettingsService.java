package com.sharshar.coinswap.services;

import org.springframework.stereotype.Service;

/**
 * Used to control the application as a whole so we can dynamically do something like pause
 * or shutdown a service
 *
 * Created by lsharshar on 8/23/2018.
 */
@Service
public class MasterSettingsService {

	/**
	 * If the system is disabled, you can't reset it without restarting the entire application
	 */
	public enum SystemStatus {
		RUNNING,
		PAUSED,
		DISABLED
	}

	private SystemStatus currentStatus;

	/**
	 * Returns the current state of the system. If it has no status, assume it's never
	 * been defined or inquired about, so assume running
	 *
	 * @return the status of the system
	 */
	public SystemStatus getStatus() {
		if (currentStatus == null) {
			currentStatus = SystemStatus.RUNNING;
		}
		return currentStatus;
	}

	public void pauseService() {
		currentStatus = SystemStatus.PAUSED;
	}

	public void resumeService() {
		if (currentStatus != SystemStatus.DISABLED) {
			currentStatus = SystemStatus.RUNNING;
		}
	}

	public void shutdown() {
		currentStatus = SystemStatus.DISABLED;
	}

	public boolean areWeRunning() {
		return currentStatus == SystemStatus.RUNNING;
	}
}
