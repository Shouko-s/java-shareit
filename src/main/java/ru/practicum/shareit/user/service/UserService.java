package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(UserDto userDto);

    List<UserDto> getAllUsers();

    UserDto getUserById(long id);

    UserDto getUserByEmail(String email);

    UserDto updateUserById(long id, UserDto userDto);

    UserDto deleteUserById(long id);
}
