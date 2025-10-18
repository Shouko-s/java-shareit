package ru.practicum.server.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.server.exception.AlreadyExists;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.user.dto.UserDto;
import ru.practicum.server.user.mapper.UserMapper;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserMapper mapper;
    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        getUserByEmailOrThrow(userDto.getEmail());

        User user = userRepository.save(mapper.dtoToUser(userDto));
        log.info("Пользователь был создан user={}", user);
        return mapper.userToDto(user);
    }

    @Override
    public UserDto getUserById(long id) {
        UserDto userDto = mapper.userToDto(getUserOrThrow(id));
        log.info("Получен пользователь с id={}", id);
        return userDto;
    }

    @Override
    public UserDto updateUserById(long id, UserDto userDto) {
        User user = getUserOrThrow(id);
        String name = userDto.getName();
        String email = userDto.getEmail();
        getUserByEmailOrThrow(email);

        if (name != null && !name.isBlank()) user.setName(name);
        if (email != null && !email.isBlank()) user.setEmail(email);

        User updated = userRepository.save(user);
        log.info("пользователь был обновлен id={}", id);
        return mapper.userToDto(updated);
    }


    @Override
    public UserDto deleteUserById(long id) {
        User user = getUserOrThrow(id);
        userRepository.deleteById(id);
        log.info("Пользователь удален id={}", id);
        return mapper.userToDto(user);
    }

    private void getUserByEmailOrThrow(String email) {
        userRepository.findByEmail(email)
                .ifPresent(ex -> {
                    throw new AlreadyExists("Такой пользователь уже существует");
                });
    }

    private User getUserOrThrow(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }
}
