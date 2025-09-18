package ru.practicum.shareit.user.mapper;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

public class UserMapper {
    public UserDto userToDto(User user) {
        return UserDto.builder()
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
