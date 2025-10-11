package ru.practicum.shareit.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.item.model.Item;

import java.util.Collection;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Collection<Item> findAllByOwnerId(Long id);

    Collection<Item> findByDescriptionContainingIgnoreCaseOrNameContainingIgnoreCaseAndAvailableTrue(String description, String name);
}
