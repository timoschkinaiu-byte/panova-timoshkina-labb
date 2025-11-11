package ru.ssau.tk.pmi.repository.manual;

import ru.ssau.tk.pmi.dto.*;

import java.util.List;
import java.util.Map;

public interface SearchService {

    // Поиск по полю
    <T> List<T> searchByField(Class<T> dtoClass, String fieldName, Object value);
    // Поиск по нескольким полям
    <T> List<T> searchByFields(Class<T> dtoClass, Map<String, Object> filters);
    // Поиск с сортировкой
    <T> List<T> searchByFieldSorted(Class<T> dtoClass, String fieldName, Object value, String sortBy, boolean ascending);
    // Поиск в глубину и в ширину
    List<FunctionDto> dfsFunctionHierarchy(Long rootFunctionId);
    List<FunctionDto> bfsFunctionHierarchy(Long rootFunctionId);
}

