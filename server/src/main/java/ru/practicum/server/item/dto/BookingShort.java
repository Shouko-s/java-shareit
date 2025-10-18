package ru.practicum.server.item.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingShort {
    private Long id;
    private Long bookerId;
}
