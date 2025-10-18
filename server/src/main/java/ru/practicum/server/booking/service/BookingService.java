package ru.practicum.server.booking.service;

import ru.practicum.server.booking.dto.BookingDto;
import ru.practicum.server.booking.dto.BookingRequest;

import java.util.List;

public interface BookingService {
    BookingDto addBooking(BookingRequest request, Long userId);

    BookingDto respond(Long bookingId, boolean approved, Long ownerId);

    BookingDto getBookingByUser(Long bookingId, Long userId);

    List<BookingDto> getAllBookingsOfUser(Long userId, String state);

    List<BookingDto> getAllBookingsOfOwner(Long userId, String state);
}
