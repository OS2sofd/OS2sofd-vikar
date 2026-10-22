package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.nexus.KMDNexusService;

@Component
@EnableScheduling
public class NexusTasks {

	@Autowired
	private KMDNexusService kmdNexusService;

	@Autowired
	private OS2VikarConfiguration config;

	@Scheduled(fixedDelay = 5 * 60 * 1000)
	public void createSubstitutes() throws Exception {
		if (!config.isScheduledJobsEnabled() || !config.getKmdNexus().isEnabled()) {
			return;
		}

		kmdNexusService.createSubstitutes();
	}
	
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	public void syncState() throws Exception {
		if (!config.isScheduledJobsEnabled() || !config.getKmdNexus().isEnabled()) {
			return;
		}

		kmdNexusService.syncStateInNexus();
	}
	
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	public void clearCache() {
		kmdNexusService.clearCache();
	}

	// hourly, starting at 01:15 - we also do delta on changes, but this catches issues lost during the day
	@Scheduled(cron = "0 15 1-22 * * ?")
	public void syncOrgUnitAssignmentsFull() {
		if (!config.isScheduledJobsEnabled() || !config.getKmdNexus().isEnabled()) {
			return;
		}

		kmdNexusService.syncOrgUnitAssignmentsFull();
	}
}
