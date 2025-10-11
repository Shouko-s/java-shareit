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
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingMapper bookingMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    @Override
    public BookingDto addBooking(BookingRequest bookingRequest, Long userId) {
        User booker = getUserOrThrow(userId);
        Item item = getItemOrThrow(bookingRequest.getItemId());

        if (item.getAvailable()) {
            Booking booking = bookingMapper.buildEntity(bookingRequest, item, booker);
            booking.setStatus(BookingStatus.WAITING);
            booking = bookingRepository.save(booking);

            bookingRepository.save(booking);
            return toDto(booking);
        }
        throw new NotAvailable("Недоступная вещь");
    }

    @Override
    public BookingDto respond(Long bookingId, boolean approve, Long ownerId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Вы не владелец");
        }

        getUserOrThrow(booking.getBooker().getId());

        if (approve) {
            booking.setStatus(BookingStatus.APPROVED);
            bookingRepository.save(booking);
        }
        return toDto(booking);
    }


    @Override
    public BookingDto getBookingByUser(Long bookingId, Long userId) {
        getUserOrThrow(userId);
        Booking booking = getBookingOrThrow(bookingId);
        Long bookerId = booking.getBooker().getId();
        Long ownerId = booking.getItem().getOwner().getId();

        if (Objects.equals(bookerId, userId) || Objects.equals(ownerId, userId)) {
            return toDto(booking);
        }

        throw new ForbiddenException("Не ваш предмет");
    }

    @Override
    public List<BookingDto> getAllBookingsOfUser(Long userId) {
        getUserOrThrow(userId);
        return bookingRepository.findAllByBookerId(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<BookingDto> getAllBookingsOfOwner(Long userId) {
        getUserOrThrow(userId);
        return bookingRepository.findAllByItem_Owner_Id(userId)
                .stream()
                .map(this::toDto)
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

    private BookingDto toDto(Booking b) {
        ItemDto itemDto = itemMapper.itemToDto(b.getItem());
        UserDto bookerDto = userMapper.userToDto(b.getBooker());
        return bookingMapper.buildDto(b, itemDto, bookerDto);
    }
}
