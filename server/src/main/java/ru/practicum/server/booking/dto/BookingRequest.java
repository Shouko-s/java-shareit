package ru.practicum.server.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BookingRequest {
    private Long itemId;
    private LocalDateTime start;
    private LocalDateTime end;
}
