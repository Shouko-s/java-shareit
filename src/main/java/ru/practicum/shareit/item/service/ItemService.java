package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemService {
    Item addItem(long ownerId, ItemDto dto);

    Item updateItem(Long userId, Long itemId, ItemDto itemDto);

    Item getItemById(Long id);

    List<Item> getAllItems(Long userId);

    List<Item> search(String text);
}
