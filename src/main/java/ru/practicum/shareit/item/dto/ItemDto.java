package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * TODO Sprint add-controllers.
 */

@Getter
@Builder
@ToString
@AllArgsConstructor
public class ItemDto {
    @NotBlank
    private String name;
    @NotBlank
    private String description;
    @NotNull
    private Boolean available;
    private Long request;
}
