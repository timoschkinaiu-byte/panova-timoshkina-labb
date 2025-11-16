package ru.ssau.tk.pmi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.ExportImportDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ExportImportController {

    @PostMapping("/functions/{id}/export")
    public ResponseEntity<Resource> exportFunction(
            @PathVariable Long id,
            @RequestParam String format) {
        // TODO: Реализовать экспорт функции
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(null);
    }

    @PostMapping(value = "/functions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExportImportDTO.ImportResponse> importFunction(
            @RequestParam(required = false) String format,
            @RequestParam("file") MultipartFile file) {
        // TODO: Реализовать импорт функции
        return ResponseEntity.ok(new ExportImportDTO.ImportResponse());
    }
}