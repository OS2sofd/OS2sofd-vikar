package dk.digitalidentity.os2vikar.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.config.modules.OrganisationSource;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubstituteOUSyncService {

	@Autowired
	private SubstituteService substituteService;
		
	@Autowired
	private WebSocketService webSocketService;

	@Autowired
	private OS2VikarConfiguration config;

	public void syncSubstituteOUs(List<Substitute> substitutes, boolean fullSync) {
		// this is a new command made just for Omada, and does not make sense for AD
		if (!config.getOrganisation().getSource().equals(OrganisationSource.OMADA)) {
			return;
		}
		
		LocalDate now = LocalDate.now();
		
		for (Substitute substitute : substitutes) {
			String ouId = null;

			// only perform sync on unmodified substitutes on full sync
			if (!fullSync && !substitute.isWorkplacesModified()) {
				continue;
			}

			log.info("Syncing OUs for substitute: " + substitute.getUsername());

			Workplace winner = null;
			if (substitute.getWorkplaces() != null) {
				for (Workplace workplace : substitute.getWorkplaces()) {
					// skip future workplaces
					if (workplace.getStartDate().isAfter(now)) {
						continue;
					}
					
					// skip ended workplaces
					if (workplace.getStopDate().isBefore(now)) {
						continue;
					}
					
					if (winner != null) {
						// skip later workplaces (we want the oldest)
						if (workplace.getStartDate().isAfter(winner.getStartDate())) {
							continue;
						}
						
						// if they are equal, we pick the one with the lowest ID
						if (workplace.getStartDate().equals(winner.getStartDate())) {
							if (workplace.getId() > winner.getId()) {
								continue;
							}
						}
					}
					
					winner = workplace;
					ouId = workplace.getOrgUnit().getUuid();
				}
			}

			if (ouId != null) {
				log.info("Setting OU to " + ouId + " for substitute: " + substitute.getUsername());

				webSocketService.updateOrgUnitForEmployee(substitute, ouId);
			}
			else {
				log.info("Not update OU (no active) for substitute: " + substitute.getUsername());
			}
		}

		// cleanup on delta-sync, so we do not handle these substitutes again
		if (!fullSync) {
			substituteService.clearWorkplacesModified(substitutes.stream().map(s -> s.getId()).collect(Collectors.toSet()));
		}
	}
}
