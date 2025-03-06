package ru.fedorov.mcp_csv_server.controller;
import ru.fedorov.mcp_csv_server.service.FileProcessingService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class FileProcessingController {

    private final WebClient webClient;
    private final FileProcessingService fileProcessingService;

    @Autowired
    public FileProcessingController(WebClient.Builder webClientBuilder, FileProcessingService fileProcessingService) {
        // Base URL for the LLM service (Ollama in this case)
        this.webClient = webClientBuilder.baseUrl("http://31.192.111.23:11434").build();
        this.fileProcessingService = fileProcessingService;
    }

    @PostMapping(value = "/process-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processFile(@RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt) {
        try {
            log.info("Enter processFile: file={}", file.getOriginalFilename());

            String fileContent = fileProcessingService.processFile(file);

            // Combine the user prompt with the file content
            String combinedPrompt = prompt + "\n\nFile Content:\n" + fileContent;

            // Prepare the JSON payload for the LLM request
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "phi4coder:latest");
            payload.put("prompt", combinedPrompt);
            payload.put("stream", false);

            log.info("Send requeset to LLM: payload={}", payload);
            
            // Call the LLM API endpoint at /api/generate
            String llmResponse = webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("LLM response: {}", llmResponse);
            return ResponseEntity.ok(llmResponse);
        } catch (Exception e) {
            log.error("Error proccessing file, return", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }
}
