package dk.digitalidentity.os2vikar.service.nexus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableCaching
public class NexusStub {
    private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Qualifier("myRestClient")
    @Autowired
    private RestClient restClient;

    @Autowired
    private NexusTokenStub tokenStub;

    @Autowired
    private OS2VikarConfiguration configuration;

    record ConflictDetails(long id) { }
    record ConflictBrokenObject(ConflictDetails details) { }
    record ConflictResponse(ConflictBrokenObject brokenObject) { }

    enum EmployeePrototypeStatus { OK, OTHER_WITH_SAME_CPR_EXISTS, TECHNICAL_ERROR, FAILED }
    record EmployeeWrapper(Employee employee, EmployeePrototypeStatus status, long existingEmployeeId) { }

    public EmployeeWrapper getEmployeePrototype(String userId) throws JsonProcessingException {
    	try {
	        String url = configuration.getKmdNexus().getNexusBaseUrl() + "/professionals/stsProfessional/prototype?query=" + userId;

	        String response = restClient.get()
	                .uri(url)
	        		.headers(h -> h.addAll(getHeaders()))
	                .retrieve()
	                .body(String.class);
	        
	        return new EmployeeWrapper(mapper.readValue(response, Employee.class), EmployeePrototypeStatus.OK, 0);
    	}
    	catch (RestClientResponseException ex) {
			String body = ex.getResponseBodyAsString();

			if (ex.getStatusCode().value() == 400) {
    			if (body != null) {
    				if (body.contains("ProfessionalWithStsSnNotFetched")) {
    					// some unknown FK Organisation error
    					log.warn("got ProfessionalWithStsSnNotFetched Nexus error on call to FK Organisation on " + userId);
    					
    					// failed is a retry
    	    	        return new EmployeeWrapper(null, EmployeePrototypeStatus.FAILED, 0);
    				}
    			}
			}
			else if (ex.getStatusCode().value() == 409) {
				long id = 0;
    			if (body != null) {
    				ConflictResponse response = mapper.readValue(body, ConflictResponse.class);
    				if (response != null && response.brokenObject() != null && response.brokenObject().details() != null) {
    					id = response.brokenObject().details().id();
    				}
    				
    				if (body.contains("ProfessionalWithSameCprAlreadyExists")) {
    	    	        return new EmployeeWrapper(null, EmployeePrototypeStatus.OTHER_WITH_SAME_CPR_EXISTS, id);
    				}
    			}
    		}
			else if (ex.getStatusCode().value() == 500) {
    			if (body != null) {
    				if (body.contains("UnsupportedOperationException")) {
    					log.warn("got UnsupportedOperationException on creating " + userId);
    					
    					// technical error is permanent
    	    	        return new EmployeeWrapper(null, EmployeePrototypeStatus.TECHNICAL_ERROR, 0);
    				}
    				else if (body.contains("http://fk-organizations/")) {
    					log.warn("got internal Nexus timeout on call to FK Organisation on " + userId);
    					
    					// failed is a retry
    	    	        return new EmployeeWrapper(null, EmployeePrototypeStatus.FAILED, 0);    					
    				}
    				// some sort of internal null pointer exception - probably because of an error in FK Org lookup, so we will attempt retry
    				else if (body.contains("500 null")) {
    					log.warn("got NPE from Nexus - probably FK Org " + userId);
    					
    					// failed is a retry
    	    	        return new EmployeeWrapper(null, EmployeePrototypeStatus.FAILED, 0);    					
    				}
    			}
    		}

    		log.error("Failed to fetch prototype for user " + userId + ", HTTP " + ex.getStatusCode().value() + ", message=" + body, ex);

    		return new EmployeeWrapper(null, EmployeePrototypeStatus.FAILED, 0);
    	}
    }
    
	public Employee getEmployee(String id) {
		String url = configuration.getKmdNexus().getNexusBaseUrl() + "/professionals/" + id;
        
        String response = restClient.get()
                .uri(url)
        		.headers(h -> h.addAll(getHeaders()))
                .retrieve()
                .body(String.class);

        try {
        	return mapper.readValue(response, Employee.class);
        }
        catch (JsonProcessingException ex) {
        	log.error("failed to process employee " + id, ex);
        }
        
        return null;
    }
    
    public EmployeeConfiguration getEmployeeConfiguration(long id) {
        String url = configuration.getKmdNexus().getNexusBaseUrl() + "/professionals/configuration/" + id;
 
        String response = null;
        try {
	        response = restClient.get()
	                .uri(url)
	        		.headers(h -> h.addAll(getHeaders()))
	                .retrieve()
	                .body(String.class);
	        
	        return mapper.readValue(response, EmployeeConfiguration.class);
        }
        catch (JsonProcessingException ex) {
        	// for debugging purposes - this happens a lot in review, and it would be nice to know why
        	log.error("Failed to parse employeeConfiguration for (" + id + "): " + response, ex);
        }
        catch (RestClientResponseException ex) {        	
        	log.error("Failed to get employeeConfiguration for " + id, ex);
        }

        return null;
    }

    @SneakyThrows
    public boolean updateEmployeeConfiguration(EmployeeConfiguration employeeConfiguration) {
        String url = configuration.getKmdNexus().getNexusBaseUrl() + "/professionals/configuration/" + employeeConfiguration.getId();
        if (employeeConfiguration.updateHref().isPresent()) {
        	url = employeeConfiguration.updateHref().get();
        }
        
        log.debug("Updating employee configuration using " + url);

    	// we need to turn into a string before sending, otherwise Apache Http Client sends as chunked, which Nexus does NOT like
    	String bodyJson = mapper.writeValueAsString(employeeConfiguration);

        try {
	        restClient.put()
                .uri(url)
        		.headers(h -> h.addAll(getHeaders()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyJson)
                .retrieve()
                .body(String.class);

	        return true;
        }
        catch (RestClientResponseException ex) {
			String body = ex.getResponseBodyAsString();

			boolean duplicateError = false;
			if (ex.getStatusCode().value() == 500) {
				// update rejected because one of the various unique fields overlaps another existing user - this _should_ fix itself
				// after some updates (when the other user is updated, it should no longer conflict, and then THIS can be updated)
				if (body != null && body.contains("Duplicate entry")) {
					duplicateError = true;
					log.warn("Failed to update employeeConfiguration " + employeeConfiguration.getId() + " because of a potential overlap with another user - message from Nexus: " + body);
				}
			}
			else if (ex.getStatusCode().value() == 400) {
				// update rejected because CPR exists on multiple users
				if (body != null && body.contains("ProfessionalServiceMultipleProfessionalReturnedByCPR")) {
					duplicateError = true;
					log.warn("Failed to update " + employeeConfiguration.getPrimaryIdentifier() + " because of error ProfessionalServiceMultipleProfessionalReturnedByCPR");
				}
				// update rejected because another user has the same primaryIdentifier
				else if (body != null && body.contains("ProfessionalServiceDataIntegrity") && body.contains("brokenObjectString=" + employeeConfiguration.getPrimaryIdentifier())) {
					duplicateError = true;
					log.warn("failed to update + " + employeeConfiguration.getPrimaryIdentifier() + " because of error ProfessionalServiceDataIntegrity");
				}
				else if (body != null && body.contains("ProfessionalPrimaryOrganizationInactive")) {
					duplicateError = true;
					log.warn("failed to update + " + employeeConfiguration.getPrimaryIdentifier() + " because they have an organisation that is inactive");
				}
			}

			if (!duplicateError) {
	        	log.error("Failed to call updateEmployeeConfiguration (payload in next logline) with response=" + body, ex);
	
	        	try {
	        		log.warn("failed request payload: " + mapper.writeValueAsString(employeeConfiguration));
	        	}
	        	catch (Exception e) {
	        		;
	        	}
			}
        }
        
        return false;
    }

    @SneakyThrows
    public boolean updateEmployee(Employee employee) {
    	try {
	        String url = configuration.getKmdNexus().getNexusBaseUrl() + "/professionals/" + employee.getId();

        	// we need to turn into a string before sending, otherwise Apache Http Client sends as chunked, which Nexus does NOT like
        	String bodyJson = mapper.writeValueAsString(employee);

	        restClient.put()
                .uri(url)
        		.headers(h -> h.addAll(getHeaders()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyJson)
                .retrieve()
                .body(String.class);
	        
	        return true;
    	}
    	catch (RestClientResponseException ex) {
			String body = ex.getResponseBodyAsString();

			boolean duplicateError = false;
			if (ex.getStatusCode().value() == 500) {
				// update rejected because one of the various unique fields overlaps another existing user - this _should_ fix itself
				// after some updates (when the other user is updated, it should no longer conflict, and then THIS can be updated)
				if (body != null && body.contains("Duplicate entry")) {
					duplicateError = true;
					log.warn("Failed to update employee " + employee.getId() + " because of a potential overlap with another user - message from Nexus: " + body);
				}
			}

			if (!duplicateError) {
	    		log.error("Failed to update employee with id " + employee.getId() + ", message=" + body, ex);
	    		
	        	try {
	        		log.warn("failed request payload: " + mapper.writeValueAsString(employee));
	        	}
	        	catch (Exception e) {
	        		;
	        	}
			}
    	}

    	return false;
    }
    
    @SneakyThrows
    public String createEmployeeFromStsPrototype(Employee prototype) {
        String response = null;
        
        String href = prototype.get_links().getCreate().getHref();
        int index = href.indexOf("/v2/");
        String endpoint = href.substring(index + 3);
        var url = configuration.getKmdNexus().getNexusBaseUrl() + endpoint;

        try {
        	// we need to turn into a string before sending, otherwise Apache Http Client sends as chunked, which Nexus does NOT like
        	String bodyJson = mapper.writeValueAsString(prototype);

	        ResponseEntity<String> responseEntity = restClient.post()
	                .uri(url)
	        		.headers(h -> h.addAll(getHeaders()))
	                .contentType(MediaType.APPLICATION_JSON)
	                .body(bodyJson)
	                .retrieve()
	                .toEntity(String.class);

	        response = responseEntity.getBody();
	        
	        try {
	        	Employee employee = mapper.readValue(responseEntity.getBody(), Employee.class);

	        	return Long.toString(employee.getId());
	        }
	        catch (Exception ex) {
	        	log.error("Could not parse response when creating: " + response);

	        	// 1 is bad data
	        	return "1";
	        }
        }
        catch (RestClientResponseException ex) {
        	String body = ex.getResponseBodyAsString();
        	
        	if (body != null && (body.contains("ProfessionalServiceDataIntegrity") || body.contains("ProfessionalServiceDuplicateProfessionalWhenTryingToCreate"))) {
        		ObjectMapper mapper = new ObjectMapper();
        		String payload = null;
        		try {
        			payload = mapper.writeValueAsString(prototype);
        		}
        		catch (Exception ignored) {
        			;
        		}

        		log.warn("Brugeren findes i forvejen - fx en bruger med samme unikId, initialer, STS UUID eller lignende. userId = " + prototype.getPrimaryIdentifier() + ", full response = " + body);
        		log.warn(" -- payload = " + payload);
        		
        		// 0 means exists already
        		return "0";
        	}
        	else if (body != null && body.contains("ProfessionalServiceProfessionalInvalidState")) {
        		ObjectMapper mapper = new ObjectMapper();
        		String payload = null;
        		try {
        			payload = mapper.writeValueAsString(prototype);
        		}
        		catch (Exception ignored) {
        			;
        		}

        		log.warn("Brugeren har dårlige data. userId = " + prototype.getPrimaryIdentifier() + ", full response = " + body);
        		log.warn(" -- payload = " + payload);
        		
        		// 1 means bad data
        		return "1";
        	}

        	// I suspect that when FK Organisation does not work, it throws some empty HTTP 500, but this text is contained currently,
        	// so we can filter it out - it might change in the future, so this has a time-to-live as a supression for alarms :)
        	if (body != null && body.contains("DefaultResponseErrorHandler.java:111")) {
        		log.warn("Failed to create employee, message=" + ex.getResponseBodyAsString(), ex);
        	}
        	else {
        		log.error("Failed to create employee, message=" + ex.getResponseBodyAsString(), ex);
        	}
       		
    		try {
    			log.info("request: " + mapper.writeValueAsString(prototype));
    		}
    		catch (Exception ignored) {
    			;
    		}
        }
        
        // retry
        return null;
    }

    @SneakyThrows
    public List<OU> getUserOrgUnits(long nexusId) {
        String url = configuration.getKmdNexus().getNexusBaseUrl() + "/professionals/" + nexusId + "/organizations";
    
        ResponseEntity<String> response = restClient.get()
                .uri(url)
        		.headers(h -> h.addAll(getHeaders()))
                .retrieve()
                .toEntity(String.class);
	        
        OU[] dto = mapper.readValue(response.getBody(), OU[].class);

        return Arrays.asList(dto);
    }

    public void changeAssignedOus(Change change, long nexusId) {
        String url = configuration.getKmdNexus().getNexusBaseUrl() + "/professionals/" + nexusId + "/organizations/changes";
        
        restClient.post()
            .uri(url)
    		.headers(h -> h.addAll(getHeaders()))
    		.body(change)
            .retrieve()
            .toEntity(String.class);
    }
    
    @CacheEvict(value = "flatNexusOUs", allEntries = true)
    public void clearCache() {
    	;
    }

    @SneakyThrows
    @Cacheable("flatNexusOUs")
    public Map<String, OU> getOusFlat() {
        String url = configuration.getKmdNexus().getNexusBaseUrl() + "/organizations/tree";
        
        ResponseEntity<String> responseEntity = restClient.get()
                .uri(url)
        		.headers(h -> h.addAll(getHeaders()))
                .retrieve()
                .toEntity(String.class);

        OU rootOU = mapper.readValue(responseEntity.getBody(), OU.class);

        List<OU> flat = new ArrayList<>();
        flattenTree(rootOU, flat);
        
        // filter inactive
        flat.removeIf(ou -> !ou.isActive());

        Map<String, OU> result = new HashMap<>();
        for (OU ou : flat) {
        	String name = ou.getName().toLowerCase();
        	if (!result.containsKey(name)) {
        		result.put(name, ou);
        	}
        	else {
        		// enable if we need to track those down
        		log.debug("Duplicate OU: " + ou.getName());
        	}
        }
        
        return result;
    }
    
	private void flattenTree(OU root, List<OU> result) {
		result.add(root);

		for (OU child : root.getChildren()) {
			flattenTree(child, result);
		}
	}
    
    private HttpHeaders getHeaders() {
    	HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + tokenStub.fetchToken());
		headers.add("Content-Type", "application/json");
		headers.add("Accept", "application/json");
		
		return headers;
	}
}
