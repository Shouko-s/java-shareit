package ru.practicum.server.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.server.request.dto.ItemRequestDto;
import ru.practicum.server.request.dto.ItemRequestResponseDto;
import ru.practicum.server.request.service.ItemRequestService;

import java.util.List;

/**
 * TODO Sprint add-item-requests.
 */
@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {

    private final ItemRequestService service;

    @PostMapping
    public ItemRequestResponseDto create(@RequestHeader("X-Sharer-User-Id") Long userId,
                                         @RequestBody ItemRequestDto dto) {
        return service.create(userId, dto);
    }

    @GetMapping
    public List<ItemRequestResponseDto> getOwn(@RequestHeader("X-Sharer-User-Id") Long userId) {
        return service.getOwn(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestResponseDto> getAll(@RequestHeader("X-Sharer-User-Id") Long userId) {
        return service.getAll(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestResponseDto getById(@RequestHeader("X-Sharer-User-Id") Long userId,
                                          @PathVariable Long requestId) {
        return service.getById(userId, requestId);
    }
}

