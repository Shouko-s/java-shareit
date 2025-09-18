package ru.practicum.shareit.user.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TODO Sprint add-controllers.
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    @NotBlank
    private String name;
    @Email
    @NotBlank
    private String email;
}
