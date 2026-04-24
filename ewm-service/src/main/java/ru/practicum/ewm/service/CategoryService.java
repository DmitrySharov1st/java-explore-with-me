package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import java.util.List;

public interface CategoryService {
    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategory(Long catId);

    CategoryDto addCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);
}