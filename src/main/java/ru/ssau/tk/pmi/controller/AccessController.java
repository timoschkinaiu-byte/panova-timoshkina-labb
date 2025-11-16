package ru.ssau.tk.pmi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.AccessDTO;

import java.util.List;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    @GetMapping
    public ResponseEntity<List<AccessDTO.Response>> getFunctionAccess(@RequestParam Long functionId) {
        // TODO: Реализовать получение списка пользователей с доступом
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<AccessDTO.Response> grantAccess(@RequestBody AccessDTO.GrantRequest request) {
        // TODO: Реализовать предоставление доступа
        return ResponseEntity.ok(new AccessDTO.Response());
    }

    @DeleteMapping
    public ResponseEntity<Void> revokeAccess(
            @RequestParam Long functionId,
            @RequestParam Long userId) {
        // TODO: Реализовать отзыв доступа
        return ResponseEntity.noContent().build();
    }
}