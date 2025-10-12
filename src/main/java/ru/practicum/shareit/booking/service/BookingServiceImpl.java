package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;
import java.util.Comparator;
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
        if (bookingRequest.getStart().isAfter(bookingRequest.getEnd())) {
            throw new ForbiddenException("start не может быть позже end");
        }
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

        return bookingRepository.findAllByBookerId(userId).stream()
                .filter(b -> switch (state == null ? "ALL" : state.toUpperCase()) {
                    case "CURRENT" -> !b.getStart().isAfter(now) && !b.getEnd().isBefore(now);
                    case "PAST" -> b.getEnd().isBefore(now);
                    case "FUTURE" -> b.getStart().isAfter(now);
                    case "WAITING" -> b.getStatus() == BookingStatus.WAITING;
                    case "REJECTED" -> b.getStatus() == BookingStatus.REJECTED;
                    case "ALL" -> true;
                    default -> true;
                })
                .sorted(Comparator.comparing(Booking::getStart).reversed())
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<BookingDto> getAllBookingsOfOwner(Long userId, String state) {
        getUserOrThrow(userId);
        log.info("Брони владельца c id={} получены, state={}", userId, state);
        LocalDateTime now = LocalDateTime.now();

        return bookingRepository.findAllByItem_Owner_Id(userId).stream()
                .filter(b -> switch (state == null ? "ALL" : state.toUpperCase()) {
                    case "CURRENT" -> !b.getStart().isAfter(now) && !b.getEnd().isBefore(now);
                    case "PAST" -> b.getEnd().isBefore(now);
                    case "FUTURE" -> b.getStart().isAfter(now);
                    case "WAITING" -> b.getStatus() == BookingStatus.WAITING;
                    case "REJECTED" -> b.getStatus() == BookingStatus.REJECTED;
                    case "ALL" -> true;
                    default -> true;
                })
                .sorted(Comparator.comparing(Booking::getStart).reversed())
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
