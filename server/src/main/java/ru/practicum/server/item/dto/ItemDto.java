package ru.practicum.server.item.dto;

import lombok.*;

import java.util.List;

/**
 * TODO Sprint add-controllers.
 */

@Getter
@Builder
@Setter
@ToString
@AllArgsConstructor
public class ItemDto {
    Long id;
    private String name;
    private String description;
    private Boolean available;
    private Long requestId;

    private BookingShort lastBooking;
    private BookingShort nextBooking;
    private List<CommentDto> comments;
}
