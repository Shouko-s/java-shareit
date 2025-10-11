package ru.practicum.shareit.booking.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequest;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

@Component
public class BookingMapper {

    public BookingDto bookingToDto(Booking booking, ItemDto itemDto, UserDto booker) {
        return BookingDto.builder()
                .start(booking.getStart())
                .end(booking.getEnd())
                .item(itemDto)
                .booker(booker)
                .status(booking.getStatus())
                .build();
    }

    public BookingDto requestToDto(BookingRequest bookingRequest, ItemDto item, UserDto booker) {
        return BookingDto.builder()
                .start(bookingRequest.getStart())
                .end(bookingRequest.getEnd())
                .item(item)
                .booker(booker)
                .build();
    }

    public Booking dtoToBooking(BookingDto bookingDto, Item item, User booker) {
        return Booking.builder()
                .start(bookingDto.getStart())
                .end(bookingDto.getEnd())
                .item(item)
                .booker(booker)
                .status(bookingDto.getStatus())
                .build();
    }
}
