package dk.digitalidentity.os2vikar.controller.mvc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.os2vikar.dao.model.Statistic;
import dk.digitalidentity.os2vikar.security.RequireStatisticsAdminAccess;
import dk.digitalidentity.os2vikar.service.StatisticService;

@Controller
@RequireStatisticsAdminAccess
public class StatisticsAdminController {
	
	@Autowired
	private StatisticService statisticService;

	public enum ChartType { BAR, PIE }

	public record SelectionOptionDTO(String label, String value) { }
	public record ChartDTO(Long id, String title, String configId, String fieldName, ChartType type) { }
	
	@GetMapping("/systemadmin/statistics")
	public String statistics(Model model) {
		List<Statistic> statistics = statisticService.getAll();
		if( statistics ==null ) {
			statistics = new ArrayList<>();
		}

		Collection<SelectionOptionDTO> selectableOus = statistics.stream()
				.collect(Collectors.toMap(Statistic::getOrgunitUuid, s ->
						new SelectionOptionDTO(s.getOrgunitName(), s.getOrgunitUuid()),
						(existing, _) -> existing))
				.values();
		Collection<SelectionOptionDTO> selectableUsers = statistics.stream()
				.collect(Collectors.toMap(Statistic::getCreatedBy, s ->
						new SelectionOptionDTO(s.getCreatedBy().replaceAll("\\s*\\([0-9a-fA-F\\-]{36}\\)$", "").trim(), s.getCreatedBy()),
						(existing, _) -> existing))
				.values();

		List<ChartDTO> availableCharts = List.of(
				new ChartDTO(1L, "Vikarer over tid", "PR_MONTH", null,  ChartType.BAR),
				new ChartDTO(2L, "Fordeling per afdeling", "DISTRIBUTION", "orgunitUuid",  ChartType.PIE),
				new ChartDTO(4L, "Fordeling per titel", "DISTRIBUTION", "title", ChartType.PIE)
		);
		model.addAttribute("availableCharts", availableCharts);
		model.addAttribute("selectableOus", selectableOus);
		model.addAttribute("selectableUsers", selectableUsers);

		return "system_admin/statistic/statistic";
	}
}
