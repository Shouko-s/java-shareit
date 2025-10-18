package ru.practicum.server.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.server.booking.dto.BookingDto;
import ru.practicum.server.booking.dto.BookingRequest;
import ru.practicum.server.booking.mapper.BookingMapper;
import ru.practicum.server.booking.model.Booking;
import ru.practicum.server.booking.model.BookingStatus;
import ru.practicum.server.booking.repository.BookingRepository;
import ru.practicum.server.exception.ForbiddenException;
import ru.practicum.server.exception.NotAvailable;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.item.dto.ItemDto;
import ru.practicum.server.item.mapper.ItemMapper;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.item.repository.ItemRepository;
import ru.practicum.server.user.dto.UserDto;
import ru.practicum.server.user.mapper.UserMapper;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
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
            booking = bookingRepository.save(booking);

            bookingRepository.save(booking);
            log.info("Бронь добавлена");
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
        if (!booking.getStatus().equals(BookingStatus.WAITING)) {
            throw new ForbiddenException("Статус заказа уже подтвержден");
        }

        if (approve) {
            booking.setStatus(BookingStatus.APPROVED);
        } else {
            booking.setStatus(BookingStatus.REJECTED);
        }
        bookingRepository.save(booking);
        log.info("Бронь подтверждена");
        return toDto(booking);
    }


    @Override
    public BookingDto getBookingByUser(Long bookingId, Long userId) {
        getUserOrThrow(userId);
        Booking booking = getBookingOrThrow(bookingId);
        Long bookerId = booking.getBooker().getId();
        Long ownerId = booking.getItem().getOwner().getId();

        if (Objects.equals(bookerId, userId) || Objects.equals(ownerId, userId)) {
            log.info("Бронь с id={} получен, пользователь c id={}", bookingId, userId);
            return toDto(booking);
        }

        throw new ForbiddenException("Не ваш предмет");
    }

    @Override
    public List<BookingDto> getAllBookingsOfUser(Long userId, String state) {
        getUserOrThrow(userId);
        log.info("Брони пользователя с id={} получен, state={}", userId, state);
        LocalDateTime now = LocalDateTime.now();
        state = (state == null ? "ALL" : state.toUpperCase());

        List<Booking> bookings;
        switch (state) {
            case "CURRENT" ->
                    bookings = bookingRepository.findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
            case "PAST" -> bookings = bookingRepository.findAllByBookerIdAndEndBeforeOrderByStartDesc(userId, now);
            case "FUTURE" -> bookings = bookingRepository.findAllByBookerIdAndStartAfterOrderByStartDesc(userId, now);
            case "WAITING" ->
                    bookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case "REJECTED" ->
                    bookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
            default -> bookings = bookingRepository.findAllByBookerIdOrderByStartDesc(userId);
        }

        return bookings.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<BookingDto> getAllBookingsOfOwner(Long userId, String state) {
        getUserOrThrow(userId);
        log.info("Брони владельца c id={} получены, state={}", userId, state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings;
        switch (state) {
            case "CURRENT" ->
                    bookings = bookingRepository.findAllByItem_Owner_IdAndStartBeforeAndEndAfterOrderByStartDesc(userId, now, now);
            case "PAST" -> bookings = bookingRepository.findAllByItem_Owner_IdAndEndBeforeOrderByStartDesc(userId, now);
            case "FUTURE" ->
                    bookings = bookingRepository.findAllByItem_Owner_IdAndStartAfterOrderByStartDesc(userId, now);
            case "WAITING" ->
                    bookings = bookingRepository.findAllByItem_Owner_IdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
            case "REJECTED" ->
                    bookings = bookingRepository.findAllByItem_Owner_IdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
            default -> bookings = bookingRepository.findAllByItem_Owner_IdOrderByStartDesc(userId);
        }
        return bookings.stream()
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
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + id + "не найдена"));
    }

    private Booking getBookingOrThrow(long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Бронь с id=" + id + "не найдена"));
    }

    private BookingDto toDto(Booking b) {
        ItemDto itemDto = itemMapper.itemToDto(b.getItem());
        UserDto bookerDto = userMapper.userToDto(b.getBooker());
        return bookingMapper.buildDto(b, itemDto, bookerDto);
    }
}
