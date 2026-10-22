package dk.digitalidentity.os2vikar.service.dto.chartJS;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartJsConfigDTO {
	private ChartJsDataDTO data;
	private TimeUnit timeUnit;
	private long sum;
}
