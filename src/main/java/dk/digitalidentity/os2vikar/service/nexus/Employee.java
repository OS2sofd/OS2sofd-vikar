package dk.digitalidentity.os2vikar.service.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// TODO: on purpose, we want to fail when they add new fields
@JsonIgnoreProperties(ignoreUnknown = false)
public class Employee {
    private long id;
    private boolean active = true;
    private int version;
    private String uid;
    private Links _links;
    
    // Dette er Profil skærmbilledet i Nexus

    // sættes ved oprettelse, og opdateres dagligt
    private String primaryIdentifier;   // Unik ID          -  sættes pt til brugernavnet på den primære AD konto
    private String initials;            // Initialer        -  konfigurabelt (enten AD brugernavn eller forbogstaver fra navn)
    private String organizationName;    // Organisation     -  navnet på den enhed deres primære tilhørsforhold peger på i SOFD
    private String unitName;            // Enhed            -  stillingsteksten på det primære tilhørsforhold i SOFD
    private ActiveDirectoryConfiguration activeDirectoryConfiguration;  // UPN, opdateres med UPN fra den primære AD konto i SOFD (kræver at UPN læses ind i SOFD, hvilket er en ny ting)
    private String identityId;										    // KMD Identity ID         - konfigurabelt om det sættes (if set, it will be set with UPN)
    private String entraIdentityId;

    // sættes kun ved oprettelse
    private String homeTelephone;       // Telefon (hjem)   -  primært telefonnummer fra SOFD
    private String mobileTelephone;     // Telefon (mobil)  -  primært telefonnummer fra SOFD
    private String telephoneHours;      // ignore til passthrough
    private String firstName;           // Fornavn
    private String middleName;          // Mellemnavn
    private String lastName;            // Efternavn
    private String fullName;            //
    private String primaryEmailAddress; // ???              - primær email adresse fra SOFD
    private String secondaryEmailAddress; // ignore til passthrough
    private String departmentName;      // Afdeling         - hardkodet værdi, opsat i kommunens konfiguration af Digital Identity 
    private Long autosignatureId;       // Autosignatur     - sætte til "Standard"

    // vi udfylder Adresselinje 1, PostNr og Landekode
    private PrimaryAddress primaryAddress;
    private PrimaryAddress secondaryAddress;

    private String loginDate;  // passthrough

    // passthrough
    private JsonNode sidConfiguration;
    private JsonNode defaultOrganizationSupplier;

    // sættes ikke, bruges bare til transport frem/tilbage
    private String workTelephone;
    private String secondaryEmailAddres;
    private User user;
}
