package ru.practicum.server.booking.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.server.booking.dto.BookingDto;
import ru.practicum.server.booking.dto.BookingRequest;
import ru.practicum.server.booking.model.Booking;
import ru.practicum.server.booking.model.BookingStatus;
import ru.practicum.server.item.dto.ItemDto;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.user.dto.UserDto;
import ru.practicum.server.user.model.User;

@Component
public class BookingMapper {
    public BookingDto buildDto(Booking booking, ItemDto itemDto, UserDto bookerDto) {
        return BookingDto.builder()
                .id(booking.getId())
                .start(booking.getStart())
                .end(booking.getEnd())
                .item(itemDto)
                .booker(bookerDto)
                .status(booking.getStatus())
                .build();
    }

    public Booking buildEntity(BookingRequest bookingRequest, Item item, User booker) {
        return Booking.builder()
                .start(bookingRequest.getStart())
                .end(bookingRequest.getEnd())
                .item(item)
                .booker(booker)
                .status(BookingStatus.WAITING)
                .build();
    }
}
