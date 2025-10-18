package ru.practicum.server.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.request.dto.ItemRequestDto;
import ru.practicum.server.request.dto.ItemRequestResponseDto;
import ru.practicum.server.request.dto.ItemResponseData;
import ru.practicum.server.request.model.ItemRequest;
import ru.practicum.server.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ItemRequestMapper {
    public ItemRequest buildEntity(ItemRequestDto dto, User requester) {
        return ItemRequest.builder()
                .description(dto.getDescription())
                .requester(requester)
                .build();
    }

    public ItemRequestResponseDto buildDto(ItemRequest request, List<Item> answers) {
        List<ItemResponseData> items = answers.stream()
                .map(i -> ItemResponseData.builder()
                        .id(i.getId())
                        .name(i.getName())
                        .ownerId(i.getOwner().getId())
                        .build())
                .collect(Collectors.toList());

        return ItemRequestResponseDto.builder()
                .id(request.getId())
                .description(request.getDescription())
                .created(request.getCreated())
                .items(items)
                .build();
    }
}
