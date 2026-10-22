package dk.digitalidentity.os2vikar.service.nexus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.os2vikar.dao.model.ADGroup;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitleADGroupMapping;
import dk.digitalidentity.os2vikar.dao.model.LocalTitle;
import dk.digitalidentity.os2vikar.dao.model.LocalTitleADGroupMapping;
import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.dao.model.Workplace;
import dk.digitalidentity.os2vikar.service.AuditLogService;
import dk.digitalidentity.os2vikar.service.GlobalTitleADGroupMappingService;
import dk.digitalidentity.os2vikar.service.SubstituteService;
import dk.digitalidentity.os2vikar.service.nexus.NexusStub.EmployeeWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KMDNexusService {

	@Autowired
	private SubstituteService substituteService;
	
	@Autowired
	private NexusStub nexusStub;
	
	@Autowired
	private GlobalTitleADGroupMappingService globalTitleADGroupMappingService;
	
	@Autowired
	private AuditLogService auditLogService;
	
	public void syncStateInNexus() {
		List<Substitute> substitutes = substituteService.getAllWithNexusId();

		for (Substitute substitute : substitutes) {
			Employee employee = nexusStub.getEmployee(substitute.getNexusId());
			if (employee != null) {
				if (shouldBeActive(substitute) && !employee.isActive()) {
					reactivateEmployee(employee);
				}
				else if (!shouldBeActive(substitute) && employee.isActive()) {
					inactivateEmployee(employee);
				}
			}
		}
	}

	private boolean shouldBeActive(Substitute substitute) {
		LocalDate now = LocalDate.now();

		for (Workplace workplace : substitute.getWorkplaces()) {
			// filter out workplaces that are no longer active
			if (workplace.getStopDate().isBefore(now)) {
				continue;
			}
			
			// filter out workplaces that are not active yet
			if (workplace.getStartDate().isAfter(now)) {
				continue;
			}
			
			return true;
		}
		
		return false;
	}

	public void createSubstitutes() {
		List<Substitute> substitutes = substituteService.getAllWithoutNexusId();
		
		for (Substitute substitute : substitutes) {
			if (createInNexus(substitute)) {
				substituteService.save(substitute);
			}
		}
	}

	private boolean createInNexus(Substitute substitute) {
		try {
			log.info("Attempting to create " + substitute.getUsername() + " in Nexus");

			EmployeeWrapper wrapper = nexusStub.getEmployeePrototype(substitute.getUsername());
			switch (wrapper.status()) {
				case FAILED:
					// just retry - data is not ready
					return false;
				case TECHNICAL_ERROR:
					log.warn("Technical error on " + substitute.getUsername());

					substitute.setNexusError("Ukendt teknisk fejl");
					return true;
				case OTHER_WITH_SAME_CPR_EXISTS:
					log.warn("Duplicate CPR on " + substitute.getUsername());

					substitute.setNexusError("Der findes en anden konto med samme CPR i Nexus: " + wrapper.existingEmployeeId());
					return true;
				case OK: {
			        String response = nexusStub.createEmployeeFromStsPrototype(wrapper.employee());
			        
			        // retry
			        if (response == null) {
			        	return false;
			        }
			        // "0" mean the user already exists in some way or other
			        else if ("0".equals(response)) {
			        	log.warn("Duplicate data on " + substitute.getUsername());
			        	
			        	substitute.setNexusError("Der findes en eksisterende bruger i Nexus der blokerer for oprettelsen af denne vikar");
			        }
			        // "1" means that is bad, and user cannot be created
			        else if ("1".equals(response)) {
			        	log.warn("Bad data on " + substitute.getUsername());
			        	
			        	substitute.setNexusError("Nexus har afvist oprettelsen grunden mangelfulde eller dårlige data i FK Organisation");
			        }
			        // created, hurray
			        else {
			        	log.info("Created in Nexus: " + substitute.getUsername());
			        	
				        substitute.setNexusId(response);
				        substitute.setNexusError(null);
			        }

					return true;					
				}
			}
		}
		catch (Exception ex) {
			log.error("Failed to create user in Nexus " + substitute.getUsername(), ex);
			
			substitute.setNexusError("Ukendt teknisk fejl");
			return true;
		}
		
		return false;
	}
	
    private void reactivateEmployee(Employee employee) {
    	employee.setActive(true);

        if (!nexusStub.updateEmployee(employee)) {
            log.error("Failed to activate Nexus employee. Could not update employee. User with userId " + employee.getPrimaryIdentifier());
            return;
    	}

    	EmployeeConfiguration employeeConfiguration = nexusStub.getEmployeeConfiguration(employee.getId());
        if (employeeConfiguration != null && !employeeConfiguration.isActive()) {
            employeeConfiguration.setActive(true);

	        if (!nexusStub.updateEmployeeConfiguration(employeeConfiguration)) {
	            log.error("Failed to activate Nexus employeeConfiguration. User with userId " + employee.getPrimaryIdentifier());
	            return;
	        }
    	}

    	log.info("Reactivated " + employee.getPrimaryIdentifier());
    }
    
	private void inactivateEmployee(Employee employee) {
        employee.setActive(false);
        if (!nexusStub.updateEmployee(employee)) {
            log.error("Failed to inactivate Nexus employee. Could not update employee. User with userId " + employee.getPrimaryIdentifier());
            return;
        }

        EmployeeConfiguration employeeConfiguration = nexusStub.getEmployeeConfiguration(employee.getId());
        if (employeeConfiguration != null && employeeConfiguration.isActive()) {
	        employeeConfiguration.setActive(false);

	        if (!nexusStub.updateEmployeeConfiguration(employeeConfiguration)) {
	            log.error("Failed to inactivate Nexus employeeConfiguration. User with userId " + employee.getPrimaryIdentifier());
	            return;
	        }
        }
        
    	log.info("Inactivated " + employee.getPrimaryIdentifier());
    }

	@Transactional(readOnly = true)
	public void syncOrgUnitAssignmentsFull() {
		List<GlobalTitleADGroupMapping> titleMappings = globalTitleADGroupMappingService.getAll();
		Map<String, OU> nexusOUs = nexusStub.getOusFlat();

		List<Substitute> substitutes = substituteService.getAll();

		for (Substitute substitute : substitutes) {
			try {
				syncSubstituteOrgUnitsToNexus(substitute, titleMappings, nexusOUs);
			}
			catch (Exception ex) {
				log.error("Failed to perform full OU sync on " + substitute.getUsername(), ex);
			}
		}
	}

	public void syncOrgUnitAssignmentsDelta(Substitute substitute) {
		try {
			log.info("Performing delta OU sync on " + substitute.getUsername());
			
			List<GlobalTitleADGroupMapping> titleMappings = globalTitleADGroupMappingService.getAll();
			Map<String, OU> nexusOUs = nexusStub.getOusFlat();

			syncSubstituteOrgUnitsToNexus(substitute, titleMappings, nexusOUs);
		}
		catch (Exception ex) {
			log.error("Failed to perform delta OU sync on " + substitute.getUsername(), ex);
		}
	}

    private void syncSubstituteOrgUnitsToNexus(Substitute substitute, List<GlobalTitleADGroupMapping> titleMappings, Map<String, OU> nexusOUs) {
    	if (!StringUtils.hasText(substitute.getNexusId())) {
    		log.warn("Substitute has no NexusID to sync: " + substitute.getUsername());
    		return;
    	}

		List<Workplace> activeWorkplaces = new ArrayList<>();
		substituteService.fillActiveWorkplaces(substitute, activeWorkplaces);
		
		// ignore if no active workplaces
		if (activeWorkplaces.size() == 0) {
			return;
		}

		Set<ADGroup> adGroups = new HashSet<>();

		for (Workplace workplace : activeWorkplaces) {
			List<LocalTitleADGroupMapping> localTitleMappings = new ArrayList<LocalTitleADGroupMapping>();

			for (LocalTitle localTitle : workplace.getOrgUnit().getLocalTitles()) {
				if (localTitle.getAdGroups() != null && localTitle.getAdGroups().size() > 0) {
					localTitleMappings.addAll(localTitle.getAdGroups());
				}
			}

			adGroups.addAll(workplace.getOrgUnit().getAdGroups().stream().map(ad -> ad.getAdGroup()).collect(Collectors.toSet()));

			if (titleMappings != null && titleMappings.size() > 0) {
				List<GlobalTitleADGroupMapping> mappingsForTitle = titleMappings.stream().filter(m -> m.getGlobalTitle().getTitle().equals(workplace.getTitle())).collect(Collectors.toList());
				for (ADGroup adGroup : mappingsForTitle.stream().map(m -> m.getAdGroup()).collect(Collectors.toList())) {
					if (adGroups.stream().noneMatch(ad -> ad.getId() == adGroup.getId())) {
						adGroups.add(adGroup);
					}
				}
			}

			if (localTitleMappings.size() > 0) {
				List<LocalTitleADGroupMapping> mappingsForTitle = localTitleMappings.stream().filter(m -> m.getLocalTitle().getTitle().equals(workplace.getTitle())).collect(Collectors.toList());
				for (ADGroup adGroup : mappingsForTitle.stream().map(m -> m.getAdGroup()).collect(Collectors.toList())) {
					if (adGroups.stream().noneMatch(ad -> ad.getId() == adGroup.getId())) {
						adGroups.add(adGroup);
					}
				}
			}
		}

		Set<Long> shouldBeAssigned = new HashSet<>();
		for (ADGroup adGroup : adGroups) {
			OU ou = nexusOUs.get(adGroup.getName().toLowerCase());
			if (ou != null) {
				shouldBeAssigned.add(ou.getId());
			}
		}
		
		Set<Long> alreadyAssigned = nexusStub.getUserOrgUnits(Long.parseLong(substitute.getNexusId())).stream().map(ou -> ou.getId()).collect(Collectors.toSet());

		// any currently assigned, 
		Set<Long> toRemove = new HashSet<Long>(alreadyAssigned);
		toRemove.removeAll(shouldBeAssigned);
		toRemove.remove(1L); // never remove root
		
		// generate log-string for toRemove
		StringBuilder toRemoveBuilder = new StringBuilder();
		if (toRemove.size() > 0) {
			for (Long id : toRemove) {
				boolean found = false;
				
				for (String ouName : nexusOUs.keySet()) {
					OU ou = nexusOUs.get(ouName);
					if (ou.getId() == id) {
						if (toRemoveBuilder.length() > 0) {
							toRemoveBuilder.append(", ");
						}
						toRemoveBuilder.append(ou.getName());
						found = true;
						break;
					}
				}
				
				if (!found) {
					log.warn("Cannot find OU with id = " + id);
				}
			}
		}
		
		shouldBeAssigned.removeAll(alreadyAssigned);
		if (shouldBeAssigned.size() > 0 || toRemove.size() > 0) {
			Change change = new Change();
			change.setAdded(shouldBeAssigned);
			change.setRemoved(toRemove);

			nexusStub.changeAssignedOus(change, Long.parseLong(substitute.getNexusId()));
			
			// compute what we want to add
			StringBuilder builder = new StringBuilder();
			builder.append("Tilføjet: ");
			for (ADGroup adGroup : adGroups) {
				OU ou = nexusOUs.get(adGroup.getName().toLowerCase());
				if (ou != null && shouldBeAssigned.contains(ou.getId())) {
					if (builder.length() > 0) {
						builder.append(", ");
					}

					builder.append(adGroup.getName());
				}
			}

			// compute what we want to log
			if (toRemoveBuilder.length() > 0) {
				builder.append(" / Fjernet: " + toRemoveBuilder.toString());
			}

			String ouString = builder.toString();
			log.info("Synchronized OUs to Nexus for " + substitute.getUsername() + " : " + ouString);

			auditLogService.syncOUsToNexus(substitute, ouString);
		}
	}

	public void clearCache() {
		nexusStub.clearCache();
	}
}
