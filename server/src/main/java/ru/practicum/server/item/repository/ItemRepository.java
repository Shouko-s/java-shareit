package ru.practicum.server.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.server.item.model.Item;

import java.util.Collection;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findAllByOwnerId(Long id);

    List<Item> findByDescriptionContainingIgnoreCaseOrNameContainingIgnoreCaseAndAvailableTrue(String description, String name);

    List<Item> findAllByItemRequestId(Long itemRequestId);

    List<Item> findAllByItemRequestIdIn(Collection<Long> itemRequestIds);

}
