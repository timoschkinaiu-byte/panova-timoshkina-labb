package ru.ssau.tk.pmi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.PointDTO;

import java.util.List;

@RestController
@RequestMapping("/api/points")
public class PointController {

    @GetMapping
    public ResponseEntity<List<PointDTO.Response>> getPoints(
            @RequestParam Long functionId,
            @RequestParam(required = false) Double xFrom,
            @RequestParam(required = false) Double xTo) {
        // TODO: Реализовать получение точек в диапазоне X
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<PointDTO.Response> createPoint(@RequestBody PointDTO.CreateRequest request) {
        // TODO: Реализовать добавление вычисленной точки
        return ResponseEntity.ok(new PointDTO.Response());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PointDTO.Response> getPointById(@PathVariable Long id) {
        // TODO: Реализовать получение точки по ID
        return ResponseEntity.ok(new PointDTO.Response());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PointDTO.Response> updatePoint(
            @PathVariable Long id,
            @RequestBody PointDTO.UpdateRequest request) {
        // TODO: Реализовать обновление точки (setY)
        return ResponseEntity.ok(new PointDTO.Response());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePoint(@PathVariable Long id) {
        // TODO: Реализовать удаление точки
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<PointDTO.Response>> searchPoints(
            @RequestParam(required = false) Double x,
            @RequestParam(required = false) Double y,
            @RequestParam(required = false) Long functionId) {
        // TODO: Реализовать поиск точек по координатам
        return ResponseEntity.ok(List.of());
    }
}
