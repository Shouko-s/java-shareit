package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.*;

@Repository
public class ItemRepoImpl implements ItemRepo {
    private final Map<Long, Item> items = new HashMap<>();


    @Override
    public Item addItem(Item item) {
        item.setId(getNextId());
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Item updateItem(Item item) {
        return items.put(item.getId(), item);
    }

    @Override
    public Optional<Item> getItemById(Long id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<Item> getAllItemsOfUser(Long userId) {
        return items.values().stream()
                .filter(item -> Objects.equals(item.getOwner().getId(), userId))
                .toList();
    }

    @Override
    public List<Item> search(String text) {
        String searchText = text.toLowerCase();
        return items.values().stream()
                .filter(Item::getAvailable)
                .filter(item -> item.getDescription().toLowerCase().contains(searchText) || item.getName().toLowerCase().contains(searchText))
                .toList();
    }


    private long getNextId() {
        long currentMaxId = items.keySet().stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);

        return ++currentMaxId;
    }
}
