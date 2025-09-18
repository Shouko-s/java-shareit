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
    public List<Item> getAllItems(Long userId) {
        return items.values().stream()
                .filter(item -> Objects.equals(item.getOwner().getId(), userId))
                .toList();
    }

    @Override
    public List<Item> search(String text) {
        List<Item> filter1 = new ArrayList<>(items.values().stream()
                .filter(item -> item.getDescription() != null)
                .filter(item -> item.getDescription().toLowerCase().contains(text) && item.getAvailable())
                .toList());

        List<Item> filter2 = items.values().stream()
                .filter(item -> item.getName() != null)
                .filter(item -> item.getName().toLowerCase().contains(text) && item.getAvailable())
                .toList();
        filter1.addAll(filter2);

        return filter1;
    }


    private long getNextId() {
        long currentMaxId = items.keySet().stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);

        return ++currentMaxId;
    }
}
