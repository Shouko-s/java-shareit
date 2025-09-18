package ru.practicum.shareit.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class UserDto {
    private String name;
    private String email;
}
