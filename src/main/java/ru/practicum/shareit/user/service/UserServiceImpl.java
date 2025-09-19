package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.AlreadyExists;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final UserMapper mapper;

    @Override
    public UserDto create(UserDto userDto) {
        getUserByEmailOrThrow(userDto.getEmail());

        User user = userRepo.create(mapper.dtoToUser(userDto));
        log.info("Пользователь был создан user={}", user);
        return mapper.userToDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        log.info("Получен список пользователей");
        return userRepo.getAllUsers().stream()
                .map(mapper::userToDto)
                .toList();
    }

    @Override
    public UserDto getUserById(long id) {
        UserDto userDto = mapper.userToDto(getUserOrThrow(id));
        log.info("Получен пользователь с id={}", id);
        return userDto;
    }

    @Override
    public UserDto getUserByEmail(String email) {
        UserDto userDto = mapper.userToDto(userRepo.getUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь с таким email не найден")));
        log.info("Пользователь был получен по email={}", email);
        return userDto;
    }

    @Override
    public UserDto updateUserById(long id, UserDto userDto) {
        User user = getUserOrThrow(id);
        String name = userDto.getName();
        String email = userDto.getEmail();
        getUserByEmailOrThrow(email);


        if ((name == null || name.isBlank()) && (email == null || email.isBlank()))
            throw new IllegalArgumentException("Требуется минимум один аргумент");
        if (name != null && !name.isBlank()) user.setName(name);
        if (email != null && !email.isBlank()) user.setEmail(email);

        User updated = userRepo.updateUserById(id, user);
        log.info("пользователь был обновлен id={}", id);
        return mapper.userToDto(updated);
    }


    @Override
    public UserDto deleteUserById(long id) {
        User user = getUserOrThrow(id);
        userRepo.deleteUserById(id);
        log.info("Пользователь удален id={}", id);
        return mapper.userToDto(user);
    }

    private void getUserByEmailOrThrow(String email) {
        userRepo.getUserByEmail(email)
                .ifPresent(ex -> {
                    throw new AlreadyExists("Такой пользователь уже существует");
                });
    }

    private User getUserOrThrow(long id) {
        return userRepo.getUserById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }
}
