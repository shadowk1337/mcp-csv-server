package ru.fedorov.mcp_csv_server.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileProcessingService {

    public String processFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        log.info("Enter processFile: file={}", filename);

        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            return parseCSV(file.getInputStream());
        } else if (filename != null
                && (filename.toLowerCase().endsWith(".xls") || filename.toLowerCase().endsWith(".xlsx"))) {
            return parseExcel(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload CSV or Excel files.");
        }
    }

    // Helper method to parse CSV files using Apache Commons CSV
    private String parseCSV(InputStream inputStream) throws Exception {
        log.info("Enter parseCSV");

        StringBuilder sb = new StringBuilder();
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new InputStreamReader(inputStream));
        for (CSVRecord record : records) {
            sb.append(record.toString()).append("\n");
        }
        return sb.toString();
    }

    // Helper method to parse Excel files using Apache POI
    private String parseExcel(InputStream inputStream) throws Exception {
        log.info("Enter parseExcel");

        StringBuilder sb = new StringBuilder();
        Workbook workbook = WorkbookFactory.create(inputStream);
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    cell.setCellType(CellType.STRING);
                    cells.add(cell.getStringCellValue());
                }
                sb.append(String.join(",", cells)).append("\n");
            }
        }
        workbook.close();
        return sb.toString();
    }
}
