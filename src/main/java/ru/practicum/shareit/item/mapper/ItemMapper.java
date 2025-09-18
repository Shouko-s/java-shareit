package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

public class ItemMapper {
    public ItemDto itemToDto(Item item) {
        return ItemDto.builder()
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .request(item.getRequest().getId()).build();
    }

    public Item dtoToItem(ItemDto dto, User owner, ItemRequest request) {
        return Item.builder()
                .id(null)
                .name(dto.getName())
                .description(dto.getDescription())
                .available(dto.getAvailable())
                .owner(owner)
                .request(request)
                .build();
    }
}
