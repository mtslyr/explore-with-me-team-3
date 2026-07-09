package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.error.BadRequestException;
import ru.practicum.ewm.common.error.NotFoundException;
import ru.practicum.ewm.common.page.OffsetPageRequest;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    @Override
    @Transactional
    public UserDto create(NewUserRequest request) {
        return UserMapper.toDto(repository.save(UserMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll(List<Long> ids, int from, int size) {
        validatePage(from, size);
        List<User> users = ids == null || ids.isEmpty()
                ? repository.findAll(new OffsetPageRequest(from, size, Sort.by("id").ascending())).getContent()
                : repository.findAllById(ids);
        return users.stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        if (!repository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        repository.deleteById(userId);
    }

    private void validatePage(int from, int size) {
        if (from < 0 || size < 1) {
            throw new BadRequestException("Invalid pagination parameters");
        }
    }
}
