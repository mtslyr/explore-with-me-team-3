package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.error.BadRequestException;
import ru.practicum.ewm.common.error.NotFoundException;
import ru.practicum.ewm.common.page.OffsetPageRequest;
import ru.practicum.ewm.event.dto.CategoryDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto request) {
        return CategoryMapper.toDto(repository.save(CategoryMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, NewCategoryDto request) {
        Category category = getCategory(catId);
        category.setName(request.getName());
        return CategoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        if (!repository.existsById(catId)) {
            throw notFound(catId);
        }
        repository.deleteById(catId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(int from, int size) {
        validatePage(from, size);
        return repository.findAll(new OffsetPageRequest(from, size, Sort.by("id").ascending())).stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(Long catId) {
        return CategoryMapper.toDto(getCategory(catId));
    }

    private Category getCategory(Long catId) {
        return repository.findById(catId)
                .orElseThrow(() -> notFound(catId));
    }

    private void validatePage(int from, int size) {
        if (from < 0 || size < 1) {
            throw new BadRequestException("Invalid pagination parameters");
        }
    }

    private NotFoundException notFound(Long catId) {
        return new NotFoundException("Category with id=" + catId + " was not found");
    }
}
