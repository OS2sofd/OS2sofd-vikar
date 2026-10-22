package dk.digitalidentity.os2vikar.service.dto.chartJS;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class ChartJsDataDTO {
	private List<String> labels;
	private List<ChartJsDatasetDTO> datasets;
}