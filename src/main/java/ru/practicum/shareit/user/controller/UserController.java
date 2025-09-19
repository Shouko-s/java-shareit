package ru.practicum.shareit.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

/**
 * TODO Sprint add-controllers.
 */
@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public UserDto create(@RequestBody @Valid UserDto userDto) {
        return userService.create(userDto);
    }

    @PatchMapping("/{id}")
    public UserDto update(@PathVariable long id, @RequestBody UserDto userDto) {
        return userService.updateUserById(id, userDto);
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable long id) {
        return userService.getUserById(id);
    }

    @DeleteMapping("/{id}")
    public UserDto deleteUserById(@PathVariable long id) {
        return userService.deleteUserById(id);
    }
}
