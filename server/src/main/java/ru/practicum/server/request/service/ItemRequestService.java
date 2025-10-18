package ru.practicum.server.request.service;

import ru.practicum.server.request.dto.ItemRequestDto;
import ru.practicum.server.request.dto.ItemRequestResponseDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestResponseDto create(Long userId, ItemRequestDto dto);

    List<ItemRequestResponseDto> getOwn(Long userId);

    List<ItemRequestResponseDto> getAll(Long userId);

    ItemRequestResponseDto getById(Long userId, Long requestId);

}
