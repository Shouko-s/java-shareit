package ru.practicum.server.request.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.server.user.model.User;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "item_requests")
public class ItemRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime created = LocalDateTime.now();
}
