package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequest;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotAvailable;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingMapper bookingMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    public BookingDto addBooking(BookingRequest bookingRequest, Long userId) {
        User booker = getUserOrThrow(userId);
        Item item = getItemOrThrow(bookingRequest.getItemId());

        if (item.getAvailable()) {
            BookingDto bookingDto = bookingMapper.requestToDto(bookingRequest, itemMapper.itemToDto(item), userMapper.userToDto(booker));
            bookingDto.setStatus(BookingStatus.WAITING);
            Booking booking = bookingMapper.dtoToBooking(bookingDto, item, booker);
            bookingRepository.save(booking);
            return bookingDto;
        }
        throw new NotAvailable("Недоступная вещь");
    }

    public BookingDto responseToBooking(Long bookingId, Boolean approve, Long ownerId) {
        User user = getUserOrThrow(ownerId);
        Booking booking = getBookingOrThrow(bookingId);
        User booker = getUserOrThrow(booking.getBooker().getId());

        if (!Objects.equals(booking.getItem().getOwner().getId(), user.getId())) {
            throw new ForbiddenException("Вы не владелец");
        }

        if (approve) {
            booking.setStatus(BookingStatus.APPROVED);
            bookingRepository.save(booking);
        } else {
            booking.setStatus(BookingStatus.REJECTED);
            bookingRepository.save(booking);
        }
        return bookingMapper.bookingToDto(booking, itemMapper.itemToDto(booking.getItem()), userMapper.userToDto(booker));
    }

    public BookingDto getBookingByBooker(Long bookingId, Long userId) {
        User user = getUserOrThrow(userId);
        Booking booking = getBookingOrThrow(bookingId);

        Long bookerId = booking.getBooker().getId();
        Long ownerId = booking.getItem().getOwner().getId();

        if (Objects.equals(bookerId, userId) || Objects.equals(ownerId, userId)) {
            return bookingMapper.bookingToDto(booking, itemMapper.itemToDto(booking.getItem()), userMapper.userToDto(user));
        }

        throw new ForbiddenException("Не ваш предмет");
    }

    public List<BookingDto> getAllBookingsOfUser(Long userId) {
        User user = getUserOrThrow(userId);
        return bookingRepository.findAllByBooker(user)
                .stream()
                .map(booking -> bookingMapper.bookingToDto(booking, itemMapper.itemToDto(booking.getItem()), userMapper.userToDto(user)))
                .toList();
    }

    private User getUserOrThrow(long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Пользователь с id=" + id + " не найден")
        );
    }

    private Item getItemOrThrow(long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));
    }

    private Booking getBookingOrThrow(long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Бронь не найдена"));
    }
}
