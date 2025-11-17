package ru.ssau.tk.pmi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.ssau.tk.pmi.dto.ExportImportDTO;
import ru.ssau.tk.pmi.dto.FunctionDTO;
import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.exceptions.FunctionNotFoundException;
import ru.ssau.tk.pmi.exceptions.InvalidFileFormatException;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;
import ru.ssau.tk.pmi.io.FunctionsIO;
import ru.ssau.tk.pmi.repository.MathFunctionRepository;
import ru.ssau.tk.pmi.repository.UserRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ExportImportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportImportController.class);
    private final MathFunctionRepository functionRepository;
    private final UserRepository userRepository;

    public ExportImportController(MathFunctionRepository functionRepository, UserRepository userRepository) {
        this.functionRepository = functionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/functions/{id}/export")
    public ResponseEntity<Resource> exportFunction(
            @PathVariable Long id,
            @RequestParam String format) {
        logger.info("Экспорт функции {} в формате: {}", id, format);

        try {
            validateExportRequest(id, format);

            MathFunction function = functionRepository.findById(id)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            TabulatedFunction tabulatedFunction = getTabulatedFunctionFromEntity(function);

            // Создаем временный файл
            String filename = generateFilename(function.getFunctionName(), format);
            Path tempFile = Files.createTempFile("function_export_", "_" + filename);

            // Экспортируем в выбранном формате
            try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                switch (format.toLowerCase()) {
                    case "serialized":
                        FunctionsIO.serialize(outputStream, tabulatedFunction);
                        break;
                    case "json":
                        exportToJson(tabulatedFunction, outputStream);
                        break;
                    case "xml":
                        exportToXml(tabulatedFunction, outputStream);
                        break;
                    default:
                        throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
                }
            }

            // Создаем Resource для скачивания
            Resource resource = new org.springframework.core.io.UrlResource(tempFile.toUri());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, getContentType(format));

            logger.info("Функция {} экспортирована в файл: {}", id, filename);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(Files.size(tempFile))
                    .body(resource);

        } catch (FunctionNotFoundException e) {
            logger.warn("Функция не найдена для экспорта: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверный формат экспорта: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка экспорта функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/functions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExportImportDTO.ImportResponse> importFunction(
            @RequestParam(required = false) String format,
            @RequestParam("file") MultipartFile file) {
        logger.info("Импорт функции из файла: {}, размер: {} байт, формат: {}",
                file.getOriginalFilename(), file.getSize(), format);

        try {
            validateImportRequest(file, format);

            TabulatedFunction tabulatedFunction;
            String detectedFormat = format != null ? format : detectFileFormat(file.getOriginalFilename());

            try (BufferedInputStream inputStream = new BufferedInputStream(file.getInputStream())) {
                switch (detectedFormat.toLowerCase()) {
                    case "serialized":
                        tabulatedFunction = FunctionsIO.deserialize(inputStream);
                        break;
                    case "json":
                        tabulatedFunction = importFromJson(inputStream);
                        break;
                    case "xml":
                        tabulatedFunction = importFromXml(inputStream);
                        break;
                    default:
                        throw new InvalidFileFormatException("Неподдерживаемый формат: " + detectedFormat);
                }
            }

            // Сохраняем импортированную функцию в БД
            String functionName = generateFunctionNameFromFile(file.getOriginalFilename());
            MathFunction savedFunction = saveTabulatedFunction(tabulatedFunction, functionName);

            ExportImportDTO.ImportResponse response = new ExportImportDTO.ImportResponse();
            response.setFunctionId(savedFunction.getFunctionId());
            response.setName(savedFunction.getFunctionName());
            response.setType("TABULATED");
            response.setPointsCount(tabulatedFunction.getCount());
            response.setFormat(detectedFormat);

            logger.info("Функция импортирована. ID: {}, точек: {}",
                    savedFunction.getFunctionId(), tabulatedFunction.getCount());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InvalidFileFormatException e) {
            logger.warn("Неверный формат файла: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.warn("Ошибка чтения файла: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка импорта функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Валидация
    private void validateExportRequest(Long functionId, String format) {
        if (functionId == null) {
            throw new IllegalArgumentException("ID функции не может быть пустым");
        }
        if (format == null || (!"json".equalsIgnoreCase(format) &&
                !"xml".equalsIgnoreCase(format) &&
                !"serialized".equalsIgnoreCase(format))) {
            throw new IllegalArgumentException("Формат должен быть: json, xml или serialized");
        }
    }

    private void validateImportRequest(MultipartFile file, String format) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }
        if (format != null && (!"json".equalsIgnoreCase(format) &&
                !"xml".equalsIgnoreCase(format) &&
                !"serialized".equalsIgnoreCase(format) &&
                !"auto".equalsIgnoreCase(format))) {
            throw new IllegalArgumentException("Формат должен быть: json, xml, serialized или auto");
        }
    }

    // Вспомогательные методы
    private String generateFilename(String functionName, String format) {
        String safeName = functionName.replaceAll("[^a-zA-Z0-9_-]", "_");
        return safeName + "." + format.toLowerCase();
    }

    private String getContentType(String format) {
        switch (format.toLowerCase()) {
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "serialized":
            default:
                return "application/octet-stream";
        }
    }

    private String detectFileFormat(String filename) {
        if (filename == null) return "serialized";

        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".json")) return "json";
        if (lowerName.endsWith(".xml")) return "xml";
        if (lowerName.endsWith(".ser") || lowerName.endsWith(".bin")) return "serialized";

        return "serialized"; // по умолчанию
    }

    private String generateFunctionNameFromFile(String filename) {
        if (filename == null) return "Импортированная_функция_" + System.currentTimeMillis();

        String name = filename.replaceAll("\\.[^.]+$", ""); // убираем расширение
        return name.isEmpty() ? "Импортированная_функция" : name;
    }



    private void exportToJson(TabulatedFunction function, OutputStream outputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, function);
    }

    private TabulatedFunction importFromJson(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, ArrayTabulatedFunction.class);
    }

    private void exportToXml(TabulatedFunction function, OutputStream outputStream) throws IOException {
        XmlMapper mapper = new XmlMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, function);
    }

    private TabulatedFunction importFromXml(InputStream inputStream) throws IOException {
        XmlMapper mapper = new XmlMapper();
        return mapper.readValue(inputStream, ArrayTabulatedFunction.class);
    }

    private TabulatedFunction getTabulatedFunctionFromEntity(MathFunction function) {
        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        function.getComputedPoints().forEach(point -> {
            xValues.add(point.getXValue());
            yValues.add(point.getYValue());
        });

        double[] xArray = xValues.stream().mapToDouble(Double::doubleValue).toArray();
        double[] yArray = yValues.stream().mapToDouble(Double::doubleValue).toArray();

        return new ArrayTabulatedFunction(xArray, yArray);
    }


    private MathFunction saveTabulatedFunction(TabulatedFunction tabulatedFunction, String name) {
        User currentUser = userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Нет пользователей в системе"));

        MathFunction mathFunction = new MathFunction();
        mathFunction.setFunctionName(name);
        mathFunction.setFunctionDefinition("Импортированная функция");
        mathFunction.setFunctionType("TABULATED");
        mathFunction.setOwner(currentUser);
        mathFunction.setIsPublic(false);
        mathFunction.setCreatedAt(LocalDateTime.now());
        mathFunction.setUpdatedAt(LocalDateTime.now());

        // СОХРАНЯЕМ ТОЧКИ
        List<ComputedPoint> points = new ArrayList<>();
        for (int i = 0; i < tabulatedFunction.getCount(); i++) {
            ComputedPoint point = new ComputedPoint();
            point.setXValue(tabulatedFunction.getX(i));
            point.setYValue(tabulatedFunction.getY(i));
            point.setFunction(mathFunction);
            points.add(point);
        }
        mathFunction.setComputedPoints(points);

        return functionRepository.save(mathFunction);
    }
}