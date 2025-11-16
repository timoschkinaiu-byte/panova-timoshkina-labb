package ru.ssau.tk.pmi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping("/register")
    public ResponseEntity<UserDTO.Response> register(@RequestBody UserDTO.RegisterRequest request) {
        // TODO: Реализовать логику регистрации
        return ResponseEntity.ok(new UserDTO.Response());
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserDTO.LoginRequest request) {
        // TODO: Реализовать логику авторизации
        return ResponseEntity.ok("token");
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO.Response> getCurrentUser() {
        // TODO: Реализовать получение текущего пользователя
        return ResponseEntity.ok(new UserDTO.Response());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO.Response> getUserById(@PathVariable Long id) {
        // TODO: Реализовать получение пользователя по ID
        return ResponseEntity.ok(new UserDTO.Response());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO.Response> updateUser(@PathVariable Long id, @RequestBody UserDTO.UpdateRequest request) {
        // TODO: Реализовать обновление пользователя
        return ResponseEntity.ok(new UserDTO.Response());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserDTO.Response> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        // TODO: Реализовать изменение роли пользователя
        return ResponseEntity.ok(new UserDTO.Response());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // TODO: Реализовать удаление пользователя
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UserDTO.ShortResponse>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        // TODO: Реализовать поиск пользователей
        return ResponseEntity.ok(List.of());
    }
}
