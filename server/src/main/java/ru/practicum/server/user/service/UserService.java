package ru.practicum.server.user.service;

import ru.practicum.server.user.dto.UserDto;

public interface UserService {
    UserDto create(UserDto userDto);

    UserDto getUserById(long id);

    UserDto updateUserById(long id, UserDto userDto);

    UserDto deleteUserById(long id);
}
