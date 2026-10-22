package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ui {
	
	// substitute pages
	private boolean showEmailOnSubstitute = true;
	private boolean showAgencyOnSubstitute = true;
	private boolean showPhoneOnSubstitute = true;
	private boolean forceCprLookup = false;
	private boolean splitSubstitutesByOrgUnits = false;
	
	// admin pages
	private boolean showDeleteSettings = true;
	
	// navbar
	private boolean showAccountPool = true;
}
