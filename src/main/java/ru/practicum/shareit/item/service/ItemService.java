package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {
    ItemDto addItem(long ownerId, ItemDto itemDto);

    ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto);

    ItemDto getItemById(Long id);

    List<ItemDto> getAllItems(Long userId);

    List<ItemDto> search(String text);

    CommentDto addComment(CommentDto commentDto, Long itemId, Long userId);
}
