package dk.digitalidentity.os2vikar.service;

import java.time.LocalDate;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SofdService {

    @Autowired
    private OS2VikarConfiguration config;

    @Autowired
    private RestClient restClient;

    public record InSofdResult(boolean inSofd, boolean hasADUser, String ADUsername) {}
    private record SofdPerson(Set<SofdAffiliation> affiliations, Set<SofdUser> users){}
    private record SofdAffiliation(String masterId, LocalDate startDate, LocalDate stopDate){}
    private record SofdUser(String userType, boolean prime, String userId){}
    public InSofdResult isInSofd(String cpr) {
    	if (!config.getSofd().isEnableLookup()) {
    		return new InSofdResult(false, false, null);
    	}
    	
        String query = "/api/v2/persons/byCpr/" + cpr;

        try {        	
            SofdPerson person = restClient
            	.get()
            	.uri(config.getSofd().getBaseUrl() + query)
	            .header("ApiKey", config.getSofd().getApiKey())
	            .retrieve()
	            .onStatus(HttpStatusCode::is4xxClientError, (_, res) -> {
	                throw new RestClientResponseException(
	                        "Client error: " + res.getStatusCode() + " : " + IOUtils.readFully(res.getBody(), res.getBody().available()),
	                        res.getStatusCode(),
	                        res.getStatusText(),
	                        null,
	                        null,
	                        null
	                );
	            })
	            .onStatus(HttpStatusCode::is5xxServerError, (_, res) -> {
	                throw new RestClientResponseException(
	                        "Server error: " + res.getStatusCode() + " : " + IOUtils.readFully(res.getBody(), res.getBody().available()),
	                        res.getStatusCode(),
	                        res.getStatusText(),
	                        null,
	                        null,
	                        null
	                );
	            })
	            .body(SofdPerson.class);


            boolean hasActiveSofdAffiliation = false;
            long affiliationCount = person.affiliations().stream().filter(a -> !a.masterId().equals(config.getSofd().getVikarMasterId()) && active(a)).count();
            if (affiliationCount > 0) {
                hasActiveSofdAffiliation = true;
            }

            SofdUser primeUser = person.users().stream().filter(u -> u.userType().equals("ACTIVE_DIRECTORY") && u.prime()).findAny().orElse(null);

            return new InSofdResult(hasActiveSofdAffiliation, primeUser != null, primeUser != null ? primeUser.userId() : null);
        }
        catch (RestClientResponseException ex) {
    		if (ex.getStatusCode().is4xxClientError() || ex.getStatusCode().is5xxServerError()) {
    			if (ex.getStatusCode().value() == 404) {
    				return new InSofdResult(false, false, null);
    			}

   				log.error("Failed to fetch employee from sofd by cpr " + cpr.substring(0, 6) + "-xxxx", ex);
    		}
    		else {
        		log.warn("Failed to fetch employee from sofd by cpr " + cpr.substring(0, 6) + "-xxxx with code: ", ex.getStatusCode());
    		}
    	}

        return null;
    }

    private boolean active(SofdAffiliation affiliation) {
        if (!notActiveYet(affiliation) && !notActiveAnymore(affiliation)) {
            return true;
        }

        return false;
    }

    private boolean notActiveYet(SofdAffiliation affiliation) {
        return (affiliation.startDate() != null && (affiliation.startDate().isAfter(LocalDate.now()) || affiliation.startDate().equals(LocalDate.now())));
    }

    private boolean notActiveAnymore(SofdAffiliation affiliation) {
        return (affiliation.stopDate() != null && (affiliation.stopDate().isBefore(LocalDate.now()) || affiliation.stopDate().equals(LocalDate.now())));
    }
}
