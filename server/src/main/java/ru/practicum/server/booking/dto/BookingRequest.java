package ru.practicum.server.booking.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BookingRequest {
    private Long itemId;
    private LocalDateTime start;
    private LocalDateTime end;
}
