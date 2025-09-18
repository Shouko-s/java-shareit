package ru.practicum.shareit.item.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

/**
 * TODO Sprint add-controllers.
 */
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public Item addItem(@Valid @RequestBody ItemDto itemDto,
                        @RequestHeader("X-Sharer-User-Id") long userId) {
        return itemService.addItem(userId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public Item updateItem(@RequestHeader("X-Sharer-User-Id") long userId,
                           @PathVariable Long itemId,
                           @RequestBody(required = false) ItemDto itemDto) {
        if (itemDto == null
                || (itemDto.getName() == null
                && itemDto.getDescription() == null
                && itemDto.getAvailable() == null)) {
            throw new IllegalArgumentException("Patch body must contain at least one field: name/description/available");
        }
        return itemService.updateItem(userId, itemId, itemDto);
    }

    @GetMapping("/{itemId}")
    public Item getItemById(@PathVariable Long itemId) {
        return itemService.getItemById(itemId);
    }

    @GetMapping
    public List<Item> getAllItems(@RequestHeader("X-Sharer-User-Id") long userId) {
        return itemService.getAllItems(userId);
    }

    @GetMapping("/search")
    public List<Item> search(@RequestParam(name = "text") String text) {
        return itemService.search(text.toLowerCase());
    }
}
