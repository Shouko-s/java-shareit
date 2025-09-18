package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public interface UserService {
    User create(User user);

    List<User> getAllUsers();

    User getUserById(long id);

    User getUserByEmail(String email);

    User updateUserById(long id, UserDto userDto);

    User deleteUserById(long id);
}
