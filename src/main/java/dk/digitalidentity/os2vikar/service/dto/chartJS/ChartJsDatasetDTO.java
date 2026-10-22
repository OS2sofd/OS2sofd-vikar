package dk.digitalidentity.os2vikar.service.dto.chartJS;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ChartJsDatasetDTO {
	private List<ChartJsDataPointDTO> data;
}