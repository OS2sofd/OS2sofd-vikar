package dk.digitalidentity.os2vikar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import dk.digitalidentity.os2vikar.service.dto.OmadaOU;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OmadaService {

	@Autowired
	private OrgUnitService orgUnitService;
	
	// make this readonly, to ensure we only save those we want to save. The issue is that
	// we have no cascade rules, and if we create new orgunits, and move existing orgunits under
	// those in the same operation, we might get unsaved-transient-exception-issues, so we
	// attempt to save only new orgunits, if those exist, and update if no new exists
	@Transactional(readOnly = true)
	public void updateOrgUnits(List<OmadaOU> sourceOrgUnits) throws Exception {
		log.info("Updating org units");

		List<String> sourceOrgUnitIds = sourceOrgUnits.stream().map(o -> o.getId()).collect(Collectors.toList());

		// validate to see if there are more than one root
		long rootCount = sourceOrgUnits.stream().filter(o -> !StringUtils.hasText(o.getParentId())).count();
		if (rootCount != 1) {
			throw new Exception("Found " + rootCount + " root OUs");
		}
		
		// extract root orgUnit
		OmadaOU parentOrg = sourceOrgUnits.stream().filter(o -> !StringUtils.hasText(o.getParentId())).findFirst().orElse(null);
		if (parentOrg == null) {
			throw new Exception("Could not find root OU");
		}

		// compare logic starts here
		List<OrgUnit> toCreate = new ArrayList<>();
		List<OrgUnit> toUpdate = new ArrayList<>();

		List<OrgUnit> dbOrgUnits = orgUnitService.getAll(o -> {
			if (o.getParent() != null) {
				o.getParent().getUuid();
			}
		});

		Map<String, OrgUnit> dbOrgUnitMap = dbOrgUnits.stream().collect(Collectors.toMap(OrgUnit::getUuid, Function.identity()));
		handleOrgUnitsRecursive(null, parentOrg, sourceOrgUnits, dbOrgUnitMap, toCreate, toUpdate);

		if (toCreate.size() > 0) {
			log.info("Creating " + toCreate.size() + " orgUnits");
			orgUnitService.saveAll(toCreate);
		}
		else if (toUpdate.size() > 0) {
			log.info("Updating " + toUpdate.size() + " orgUnits");
			orgUnitService.saveAll(toUpdate);
		}

		// Handle deletion
		List<OrgUnit> toBeDeleted = dbOrgUnits.stream().filter(o -> !sourceOrgUnitIds.contains(o.getUuid())).collect(Collectors.toList());
		if (toBeDeleted.size() > 0) {
			log.info("Deleting " + toBeDeleted.size() + " orgUnits");
			
			log.info("deleting: " + String.join(",", toBeDeleted.stream().map(o -> o.getName()).toList()));

			orgUnitService.deleteAll(toBeDeleted);
		}

		log.info("Finished updating org units");
	}

	private void handleOrgUnitsRecursive(OrgUnit parent, OmadaOU sourceCurrent, List<OmadaOU> sourceOrgUnits, Map<String, OrgUnit> dbOrgUnitMap, List<OrgUnit> toCreate, List<OrgUnit> toUpdate) {		
		OrgUnit temp = dbOrgUnitMap.get(sourceCurrent.getId());
		if (temp == null) {
			temp = new OrgUnit();
			temp.setUuid(sourceCurrent.getId());
			temp.setParent(parent);
			temp.setName(sourceCurrent.getName());
			
			log.info("OU '" + temp.getName() + "' was created");

			toCreate.add(temp);
		}
		else {
			boolean changes = false;

			if (!Objects.equals(temp.getName(), sourceCurrent.getName())) {
				log.info("OU '" + temp.getName() + "' had a name change to " + sourceCurrent.getName());

				temp.setName(sourceCurrent.getName());
				changes = true;				
			}
			
			if (temp.getParent() == null && parent != null) {
				log.info("OU '" + temp.getName() + "' got a parent: " + parent.getName());

				temp.setParent(parent);
				changes = true;
			}
			else if (temp.getParent() != null && parent == null) {
				log.info("OU '" + temp.getName() + "' lost a parent");

				temp.setParent(null);
				changes = true;
			}
			else if (temp.getParent() != null && parent != null) {
				if (!Objects.equals(temp.getParent().getUuid(), parent.getUuid())) {
					log.info("OU '" + temp.getName() + "' got a new parent: " + parent.getName());

					temp.setParent(parent);
					changes = true;
				}
			}

			if (changes) {
				toUpdate.add(temp);
			}
		}

		List<OmadaOU> children = sourceOrgUnits.stream().filter(o -> o.getParentId() != null && o.getParentId().equalsIgnoreCase(sourceCurrent.getId())).collect(Collectors.toList());

		for (OmadaOU child : children) {
			handleOrgUnitsRecursive(temp, child, sourceOrgUnits, dbOrgUnitMap, toCreate, toUpdate);
		}
	}
}
