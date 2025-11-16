package ru.ssau.tk.pmi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.OperationDTO;
import ru.ssau.tk.pmi.dto.FunctionDTO;

@RestController
@RequestMapping("/api/operations")
public class OperationController {

    @PostMapping("/add")
    public ResponseEntity<FunctionDTO.Response> addFunctions(@RequestBody OperationDTO.BinaryOperationRequest request) {
        // TODO: Реализовать сложение функций
        return ResponseEntity.ok(new FunctionDTO.Response());
    }

    @PostMapping("/subtract")
    public ResponseEntity<FunctionDTO.Response> subtractFunctions(@RequestBody OperationDTO.BinaryOperationRequest request) {
        // TODO: Реализовать вычитание функций
        return ResponseEntity.ok(new FunctionDTO.Response());
    }

    @PostMapping("/multiply")
    public ResponseEntity<FunctionDTO.Response> multiplyFunctions(@RequestBody OperationDTO.BinaryOperationRequest request) {
        // TODO: Реализовать умножение функций
        return ResponseEntity.ok(new FunctionDTO.Response());
    }

    @PostMapping("/divide")
    public ResponseEntity<FunctionDTO.Response> divideFunctions(@RequestBody OperationDTO.BinaryOperationRequest request) {
        // TODO: Реализовать деление функций
        return ResponseEntity.ok(new FunctionDTO.Response());
    }

    @PostMapping("/differentiate")
    public ResponseEntity<FunctionDTO.Response> differentiateFunction(@RequestBody OperationDTO.UnaryOperationRequest request) {
        // TODO: Реализовать дифференцирование функции
        return ResponseEntity.ok(new FunctionDTO.Response());
    }

    @PostMapping("/integrate")
    public ResponseEntity<OperationDTO.IntegrateResponse> integrateFunction(@RequestBody OperationDTO.IntegrateRequest request) {
        // TODO: Реализовать интегрирование функции
        return ResponseEntity.ok(new OperationDTO.IntegrateResponse());
    }
}
