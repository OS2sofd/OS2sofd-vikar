package dk.digitalidentity.os2vikar.service.xls;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.View;

import dk.digitalidentity.os2vikar.dao.model.Statistic;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StatisticXlsView implements View {
	private static final String CONTENT_TYPE = "application/ms-excel";
	private int rowCount = 1;

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}
	
	@Override
	public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		@SuppressWarnings("unchecked")
		List<Statistic> statistics = (List<Statistic>) model.get("statistics");

		response.setContentType(getContentType());
		response.setHeader("Content-Disposition", "attachment; filename=\"statistik.xlsx\"");

		try (Workbook workbook = new DisposableSXSSFWorkbook()) {

			// create excel xls sheet
			Sheet sheet = workbook.createSheet("Vikarstatistik");
	
			// create header row
			createHeader(workbook, sheet);
	
			for (Statistic statistic : statistics) {
				Row row = sheet.createRow(rowCount++);
	
				row.createCell(0).setCellValue(statistic.getSubstituteName());
				row.createCell(1).setCellValue(statistic.getSubstituteUserId());
				row.createCell(2).setCellValue(statistic.getOrgunitName());
				row.createCell(3).setCellValue(statistic.getOrgunitUuid());
				row.createCell(4).setCellValue(statistic.getStartDate().toString());
				row.createCell(5).setCellValue(statistic.getStopDate().toString());
				row.createCell(6).setCellValue(statistic.getCreatedBy());
			}
			
			workbook.write(response.getOutputStream());
		}
	}

	private void createHeader(Workbook workbook, Sheet sheet) {
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, "Vikar", headerStyle);
		createCell(header, 1, "Brugernavn", headerStyle);
		createCell(header, 2, "Enhed", headerStyle);
		createCell(header, 3, "Enhed UUID", headerStyle);
		createCell(header, 4, "Start dato", headerStyle);
		createCell(header, 5, "Stop dato", headerStyle);
		createCell(header, 6, "Oprettet af", headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
