package dk.digitalidentity.os2vikar.service.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfessionalJob {
    private long id;
    private String name;
    private boolean active;
}
