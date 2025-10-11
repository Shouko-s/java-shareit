package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank
    private String name;
    @NotBlank
    private String description;
    @NotNull
    private Boolean available;

    private BookingShort lastBooking;
    private BookingShort nextBooking;
    private List<CommentDto> comments;
}
