package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.event.dto.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto request);

    CategoryDto update(Long catId, NewCategoryDto request);

    void delete(Long catId);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long catId);
}
