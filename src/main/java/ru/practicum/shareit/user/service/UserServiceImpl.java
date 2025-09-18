package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.AlreadyExists;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;

    @Override
    public User create(User user) {
        getUserByEmailOrThrow(user.getEmail());
        return userRepo.create(user);
    }

    @Override
    public List<User> getAllUsers() {
        return null;
    }

    @Override
    public User getUserById(long id) {
        return userRepo.getUserById(id).orElseThrow(() -> new NotFoundException("Не существует"));
    }

    @Override
    public User getUserByEmail(String email) {
        return null;
    }

    @Override
    public User updateUserById(long id, UserDto userDto) {
        User user = getUserOrThrow(id);
        getUserByEmailOrThrow(userDto.getEmail());
        String name = userDto.getName();
        String email = userDto.getEmail();

        if (name == null && email == null) throw new IllegalArgumentException();
        if (name != null) user.setName(name);
        if (email != null) user.setEmail(email);

        return user;
    }


    @Override
    public User deleteUserById(long id) {
        getUserOrThrow(id);
        return userRepo.deleteUserById(id);
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
