package ru.practicum.shareit.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequest;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

/**
 * TODO Sprint add-bookings.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/bookings")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public BookingDto createBooking(@Valid @RequestBody BookingRequest bookingRequest,
                                    @RequestHeader("X-Sharer-User-Id") Long userId) {
        return bookingService.addBooking(bookingRequest, userId);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto responseToBooking(@PathVariable Long bookingId,
                                        @RequestParam(name = "approved") Boolean approved,
                                        @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return bookingService.respond(bookingId, approved, ownerId);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getBookingByBooker(@PathVariable Long bookingId,
                                         @RequestHeader("X-Sharer-User-Id") Long bookerId) {
        return bookingService.getBookingByUser(bookingId, bookerId);
    }

    @GetMapping
    public List<BookingDto> getAllBookingsOfUser(@RequestHeader("X-Sharer-User-Id") Long userId) {
        return bookingService.getAllBookingsOfUser(userId);
    }

    @GetMapping("/owner")
    public List<BookingDto> listForOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return bookingService.getAllBookingsOfOwner(ownerId);
    }
}
