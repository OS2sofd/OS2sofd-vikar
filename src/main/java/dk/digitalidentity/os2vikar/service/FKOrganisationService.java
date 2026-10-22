package dk.digitalidentity.os2vikar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import dk.digitalidentity.os2vikar.service.dto.FKHierarchyWrapper;
import dk.digitalidentity.os2vikar.service.dto.FKOU;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FKOrganisationService {
	// TODO: rewrite to RestClient
	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private OS2VikarConfiguration config;

	@Autowired
	private OrgUnitService orgUnitService;

	public void updateOrgUnits() throws Exception {
		log.info("Updating org units");

		List<FKOU> sourceOrgUnits;
		List<String> sourceOrgUnitsUuids;
		
		try {
			sourceOrgUnits = getOrgUnits();
			sourceOrgUnitsUuids = sourceOrgUnits.stream().map(o -> o.getUuid()).collect(Collectors.toList());
		}
		catch (Exception ex) {
			log.warn("Failed to get data for orgunit update", ex);
			return;
		}

		// extract root orgUnit
		FKOU parentOrg = sourceOrgUnits.stream().filter(o -> o.getParentOU() == null).findFirst().orElse(null);
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

		// if there are new ones to create, we ONLY create, and skip updating, because Hibernate is a little bitch, and
		// I can't figure out how to make it keep crying when we both create new orgUnits AND move existing ones below
		// the newly created ones... so it will take 2 runs to come into sync in that case
		if (toCreate.size() > 0) {
			log.info("Creating " + toCreate.size() + " orgUnits");

			// we will probably still get the Hibernate exception here, but at least the Propagation annotation
			// will ensure that the data is saved, so next run will not throw an exception
			orgUnitService.saveAll(toCreate);
		}
		else if (toUpdate.size() > 0) {
			log.info("Updating " + toUpdate.size() + " orgUnits");

			orgUnitService.saveAll(toUpdate);
		}

		// Handle deletion
		List<OrgUnit> toBeDeleted = dbOrgUnits.stream().filter(o -> !sourceOrgUnitsUuids.contains(o.getUuid())).collect(Collectors.toList());
		if (toBeDeleted.size() > 0) {
			log.info("Deleting " + toBeDeleted.size() + " orgUnits");
			
			log.info("deleting: " + String.join(",", toBeDeleted.stream().map(o -> o.getName()).toList()));

			orgUnitService.deleteAll(toBeDeleted);
		}

		log.info("Finished updating org units");
	}

	private List<FKOU> getOrgUnits() throws Exception {
		log.info("Attempting to fetch organisation data from FK Organisation");

		var headers = new HttpHeaders();
		headers.add("Cvr", config.getCvr());
		headers.add("onlyOUs", "true");
		HttpEntity<String> request = new HttpEntity<>(headers);

		ResponseEntity<String> keyResponse = restTemplate.exchange(config.getOrganisation().getUrl(), HttpMethod.GET, request, String.class);

		if (keyResponse.getStatusCode().value() != 200) {
			throw new Exception("Synchronization (getKey) failed: " + keyResponse.getStatusCode().value());
		}

		String key = keyResponse.getBody().replace("\"", "");

		ResponseEntity<FKHierarchyWrapper> response = null;
		var maxAttempts = 60;

		for (int i = 1; i <= maxAttempts; i++) {
			Thread.sleep(5 * 1000); // sleep 5 seconds before attempting to read again

			try {
				response = restTemplate.getForEntity(config.getOrganisation().getUrl() + "/" + key, FKHierarchyWrapper.class);

				if (response.getStatusCode().value() != 404) {
					break;
				}
			}
			catch (RestClientException e) {
				log.warn("Failed to get hierarchy for key " + key + ". Attempt " + i + " of " + maxAttempts + ".");
			}
		}

		if (response == null) {
			log.warn("Synchronization (getResponse) failed: Timeout");
			throw new Exception("Synchronization (getResponse) failed: Timeout");
		}

		if (response.getStatusCode().value() != 200) {
			log.warn("Synchronization (getResponse) failed: " + response.getStatusCode().value());
			throw new Exception("Synchronization (getResponse) failed: " + response.getStatusCode().value());
		}

		FKHierarchyWrapper hierarchyWrapper = response.getBody();

		if (hierarchyWrapper.getStatus() != 0) {
			throw new Exception("Synchronization (HierarchyStatus) failed: " + hierarchyWrapper.getStatus());
		}

		var hierarchy = hierarchyWrapper.getResult();
		log.info("Successfully fetched orgunit Hierarcy. OU count: " + hierarchy.getOus().size() + ".");

		return hierarchy.getOus();
	}

	private void handleOrgUnitsRecursive(OrgUnit parent, FKOU sourceCurrent, List<FKOU> sourceOrgUnits, Map<String, OrgUnit> dbOrgUnitMap, List<OrgUnit> toCreate, List<OrgUnit> toUpdate) {		
		OrgUnit temp = dbOrgUnitMap.get(sourceCurrent.getUuid());
		if (temp == null) {
			temp = new OrgUnit();
			temp.setUuid(sourceCurrent.getUuid());
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

		List<FKOU> children = sourceOrgUnits.stream().filter(o -> o.getParentOU() != null && o.getParentOU().equalsIgnoreCase(sourceCurrent.getUuid())).collect(Collectors.toList());

		for (FKOU child : children) {
			handleOrgUnitsRecursive(temp, child, sourceOrgUnits, dbOrgUnitMap, toCreate, toUpdate);
		}
	}
}
