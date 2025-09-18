package ru.practicum.shareit.item.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

/**
 * TODO Sprint add-controllers.
 */

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Item {
    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private User owner;
    private ItemRequest request;
}
