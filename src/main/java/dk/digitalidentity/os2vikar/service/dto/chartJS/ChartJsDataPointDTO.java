package dk.digitalidentity.os2vikar.service.dto.chartJS;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ChartJsDataPointDTO {
	private String x;
	private Long y;
}
