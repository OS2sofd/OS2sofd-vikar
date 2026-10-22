package dk.digitalidentity.os2vikar.service.nexus;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
//TODO: on purpose, we want to fail when they add new fields
@JsonIgnoreProperties(ignoreUnknown = false)
public class EmployeeConfiguration {
    private int id;
    private int version;
    private String stsSn;
    private boolean active = true;
    private String primaryIdentifier;

    // sættes ikke, bruges bare til transport frem/tilbage
    private String cpr;
    private boolean replyToDefaultMedcomSenderOrganization;
    private JsonNode primaryOrganization;
    private Long defaultMedcomSenderOrganizationId;
    private String identityId;
    private JsonNode nationalRoleConfiguration;
    private String color;
    private String entraIdentityId;
    private String basisReportUsername;
    private JsonNode kmdVagtplanConfiguration;
    private JsonNode smdbConfiguration;
    private JsonNode roadTimesCalculationConfiguration;
    private JsonNode defaultOrganizationSupplier;
    private JsonNode activeDirectoryConfiguration;
    private JsonNode authorizationCodeConfiguration;
    private JsonNode exchangeConfiguration;
    private JsonNode professionalJob;
    private JsonNode eventPlaningConfiguration;
    private JsonNode sidConfigurationResource;
    private JsonNode patientBlackList;
    
    @JsonProperty("_links")
    private Map<String, Link> links = Map.of();
    
    public Optional<String> linkHref(String rel) {
        return Optional.ofNullable(links.get(rel)).map(Link::href);
    }

    public Optional<String> updateHref() {
        return linkHref("update");
    }
}
