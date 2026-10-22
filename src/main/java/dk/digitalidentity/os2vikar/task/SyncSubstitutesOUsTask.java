package dk.digitalidentity.os2vikar.task;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.config.modules.OrganisationSource;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.service.SubstituteOUSyncService;
import dk.digitalidentity.os2vikar.service.SubstituteService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class SyncSubstitutesOUsTask {

	@Autowired
	private SubstituteService substituteService;

	@Autowired
	private SubstituteOUSyncService substituteOUSyncService;

	@Autowired
	private OS2VikarConfiguration config;

	// run every 10 minutes, to make sure the orgsystem (usually Omada) knows where the substitute is supposed to be
	@Scheduled(cron = "${cron.substitute.ous.deltasync:0 0/10 * * * ?}")
	public void execute() throws Exception {
		if (!config.isScheduledJobsEnabled() || !config.getOrganisation().getSource().equals(OrganisationSource.OMADA)) {
			return;
		}

		List<Substitute> substitutes = substituteService.getSubstitutesForOUSync(false);
		if (substitutes.size() == 0) {
			return;
		}

		substituteOUSyncService.syncSubstituteOUs(substitutes, false);
	}
	
	// run every morning to handle future/expired workplaces (02:15 to deal with potential timezones and summer/winter-time)
	@Scheduled(cron = "${cron.substitute.ous.fullsync:0 15 2 * * ?}")
	public void executeMorning() throws Exception {
		if (!config.isScheduledJobsEnabled() || !config.getOrganisation().getSource().equals(OrganisationSource.OMADA)) {
			return;
		}

		log.info("SyncEmployeesOUsTask task started (full)");

		List<Substitute> substitutes = substituteService.getSubstitutesForOUSync(true);
		if (substitutes.size() == 0) {
			return;
		}

		substituteOUSyncService.syncSubstituteOUs(substitutes, true);
		
		log.info("SyncEmployeesOUsTask task ended (full)");
	}
}
