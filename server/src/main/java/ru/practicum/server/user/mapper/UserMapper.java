package ru.practicum.server.user.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.server.user.dto.UserDto;
import ru.practicum.server.user.model.User;

@Component
public class UserMapper {
    public UserDto userToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public User dtoToUser(UserDto userDto) {
        return User.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .email(userDto.getEmail())
                .build();
    }
}

