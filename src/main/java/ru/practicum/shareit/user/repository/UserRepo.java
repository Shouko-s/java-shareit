package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.user.model.User;

import java.util.Optional;

public interface UserRepo {
    User create(User user);

    Optional<User> getUserById(long id);

    Optional<User> getUserByEmail(String email);

    User updateUserById(long id, User user);

    User deleteUserById(long id);
}
