package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDateTime;

public interface AuditLogProjection {
	public LocalDateTime getTts();
	public String getAdministrator();
	public String getSubstitute();
	public String getOperation();
	public String getDetails();
	public String getStartDate();
	public String getStopDate();
}
