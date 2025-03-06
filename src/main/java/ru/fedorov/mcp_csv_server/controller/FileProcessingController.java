import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class FileProcessingController {
    private final WebClient webClient;

    @Autowired
    public FileProcessingController(WebClient.Builder webClientBuilder) {
        // Base URL for the LLM service (Ollama in this case)
        this.webClient = webClientBuilder.baseUrl("http:/ /31.192.111.23:11434").build();
    }

    @PostMapping(value = "/process-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processFile(@RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt) {
        try {
            String fileContent;
            String filename = file.getOriginalFilename();
            if (filename != null && filename.toLowerCase().endsWith(".csv")) {
                fileContent = parseCSV(file.getInputStream());
            } else if (filename != null &&
                    (filename.toLowerCase().endsWith(".xls") || filename.toLowerCase().endsWith(".xlsx"))) {
                fileContent = parseExcel(file.getInputStream());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Unsupported file type. Please upload CSV or Excel files.");
            }

            // Combine the user prompt with file content.
            String combinedPrompt = prompt +
                    "\n\nFile Content:\n"
                    + fileContent;

            // Call the LLM API. Adjust the endpoint and request format as per your LLM service.
            String llmResponse = webClient.post()
                    .uri("/") // Change the URI if needed
                    .bodyValue(combinedPrompt).retrieve().bodyToMono(String.class).block();
            return ResponseEntity.ok(llmResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file:" + e.getMessage());
        }
    }

    // Helper method to parse CSV files using Apache Commons CSV
    private String parseCSV(InputStream inputStream) throws Exception {
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
