package ru.practicum.server.item.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.server.request.model.ItemRequest;
import ru.practicum.server.user.model.User;

/**
 * TODO Sprint add-controllers.
 */

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Getter
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(name = "is_available", nullable = false)
    private Boolean available;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_request_id")
    private ItemRequest itemRequest;
}
