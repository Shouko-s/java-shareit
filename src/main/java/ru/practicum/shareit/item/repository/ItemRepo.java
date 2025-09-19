package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepo {
    Item addItem(Item item);

    Item updateItem(Item item);

    Optional<Item> getItemById(Long id);

    List<Item> getAllItemsOfUser(Long userId);

    List<Item> search(String text);
}
