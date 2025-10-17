package ru.practicum.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import ru.practicum.server.booking.dto.BookingDto;
import ru.practicum.server.booking.dto.BookingRequest;
import ru.practicum.server.booking.mapper.BookingMapper;
import ru.practicum.server.booking.model.Booking;
import ru.practicum.server.booking.model.BookingStatus;
import ru.practicum.server.booking.repository.BookingRepository;
import ru.practicum.server.booking.service.BookingServiceImpl;
import ru.practicum.server.exception.ForbiddenException;
import ru.practicum.server.exception.NotAvailable;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.item.mapper.ItemMapper;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.item.repository.ItemRepository;
import ru.practicum.server.user.mapper.UserMapper;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = BookingServiceImpl.class)
@Import({BookingMapper.class, ItemMapper.class, UserMapper.class})
class BookingServiceTests {

    @MockBean
    private BookingRepository bookingRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ItemRepository itemRepository;

    private static User user(long id, String name) {
        return User.builder().id(id).name(name).email(name + "@ex.com").build();
    }

    private static Item item(long id, String name, boolean available, User owner) {
        return Item.builder().id(id).name(name).available(available).owner(owner).build();
    }

    private static Booking booking(long id, Item it, User booker, LocalDateTime start, LocalDateTime end, BookingStatus st) {
        return Booking.builder().id(id).item(it).booker(booker).start(start).end(end).status(st).build();
    }

    private static BookingRequest req(long itemId) {
        return BookingRequest.builder()
                .itemId(itemId)
                .start(LocalDateTime.now().plusHours(1))
                .end(LocalDateTime.now().plusHours(2))
                .build();
    }

    // ======= твои существующие тесты (без изменений) =======

    @Test
    @DisplayName("addBooking: успех при доступной вещи (двойной save)")
    void addBookingSuccess() {
        long ownerId = 1L;
        long bookerId = 2L;
        long itemId = 10L;
        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");
        Item it = item(itemId, "Дрель", true, owner);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(it));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        BookingDto dto = svc.addBooking(req(itemId), bookerId);

        assertThat(dto).isNotNull();
        assertThat(dto.getItem().getId()).isEqualTo(itemId);
        assertThat(dto.getBooker().getId()).isEqualTo(bookerId);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(2)).save(cap.capture());
        Booking lastSaved = cap.getAllValues().get(1);
        assertThat(lastSaved.getItem().getId()).isEqualTo(itemId);
        assertThat(lastSaved.getBooker().getId()).isEqualTo(bookerId);
    }

    @Test
    @DisplayName("addBooking: вещь недоступна -> NotAvailable")
    void addBookingItemNotAvailable() {
        long ownerId = 1L;
        long bookerId = 2L;
        long itemId = 10L;
        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");
        Item it = item(itemId, "Дрель", false, owner);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(it));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        assertThatThrownBy(() -> svc.addBooking(req(itemId), bookerId))
                .isInstanceOf(NotAvailable.class)
                .hasMessageContaining("Недоступная вещь");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("addBooking: NotFound по пользователю и по вещи")
    void addBookingNotFoundBranches() {
        long bookerId = 2L;
        long itemId = 10L;

        when(userRepository.findById(bookerId)).thenReturn(Optional.empty());
        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );
        assertThatThrownBy(() -> svc.addBooking(req(itemId), bookerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь");

        User booker = user(bookerId, "booker");
        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.addBooking(req(itemId), bookerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Вещь");
    }

    @Test
    @DisplayName("getAllBookingsOfUser: ALL/CURRENT/WAITING — корректные вызовы и маппинг")
    void getAllBookingsOfUserStates() {
        long userId = 33L;
        User booker = user(userId, "booker");
        when(userRepository.findById(userId)).thenReturn(Optional.of(booker));

        User owner = user(1L, "owner");
        Item it = item(100L, "Дрель", true, owner);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> all = List.of(
                booking(1L, it, booker, now.minusDays(2), now.minusDays(1), BookingStatus.APPROVED),
                booking(2L, it, booker, now.plusDays(1), now.plusDays(2), BookingStatus.WAITING)
        );
        when(bookingRepository.findAllByBookerIdOrderByStartDesc(userId)).thenReturn(all);

        List<Booking> current = List.of(
                booking(3L, it, booker, now.minusHours(1), now.plusHours(1), BookingStatus.APPROVED)
        );
        when(bookingRepository.findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(
                eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(current);

        List<Booking> waiting = List.of(
                booking(4L, it, booker, now.plusHours(2), now.plusHours(3), BookingStatus.WAITING)
        );
        when(bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(
                eq(userId), eq(BookingStatus.WAITING)))
                .thenReturn(waiting);

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        List<BookingDto> listAll = svc.getAllBookingsOfUser(userId, null);
        assertThat(listAll).hasSize(2);
        verify(bookingRepository).findAllByBookerIdOrderByStartDesc(userId);

        List<BookingDto> listCurrent = svc.getAllBookingsOfUser(userId, "CURRENT");
        assertThat(listCurrent).hasSize(1);
        assertThat(listCurrent.get(0).getId()).isEqualTo(3L);
        verify(bookingRepository).findAllByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(
                eq(userId), any(LocalDateTime.class), any(LocalDateTime.class));

        List<BookingDto> listWaiting = svc.getAllBookingsOfUser(userId, "WAITING");
        assertThat(listWaiting).hasSize(1);
        assertThat(listWaiting.get(0).getStatus()).isEqualTo(BookingStatus.WAITING);
        verify(bookingRepository).findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
    }

    @Test
    @DisplayName("getAllBookingsOfOwner: ALL/PAST/REJECTED — корректные вызовы и маппинг")
    void getAllBookingsOfOwnerStates() {
        long ownerId = 44L;
        User owner = user(ownerId, "owner");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        User booker = user(77L, "booker");
        Item it = item(200L, "Лобзик", true, owner);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> all = List.of(
                booking(10L, it, booker, now.minusDays(3), now.minusDays(2), BookingStatus.APPROVED),
                booking(11L, it, booker, now.plusDays(1), now.plusDays(2), BookingStatus.REJECTED)
        );
        when(bookingRepository.findAllByItem_Owner_IdOrderByStartDesc(ownerId)).thenReturn(all);

        List<Booking> past = List.of(
                booking(12L, it, booker, now.minusDays(5), now.minusDays(4), BookingStatus.APPROVED)
        );
        when(bookingRepository.findAllByItem_Owner_IdAndEndBeforeOrderByStartDesc(
                eq(ownerId), any(LocalDateTime.class)))
                .thenReturn(past);

        List<Booking> rejected = List.of(
                booking(13L, it, booker, now.plusHours(5), now.plusHours(6), BookingStatus.REJECTED)
        );
        when(bookingRepository.findAllByItem_Owner_IdAndStatusOrderByStartDesc(
                eq(ownerId), eq(BookingStatus.REJECTED)))
                .thenReturn(rejected);

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        List<BookingDto> listAll = svc.getAllBookingsOfOwner(ownerId, "ALL");
        assertThat(listAll).hasSize(2);
        verify(bookingRepository).findAllByItem_Owner_IdOrderByStartDesc(ownerId);

        List<BookingDto> listPast = svc.getAllBookingsOfOwner(ownerId, "PAST");
        assertThat(listPast).hasSize(1);
        assertThat(listPast.get(0).getId()).isEqualTo(12L);
        verify(bookingRepository).findAllByItem_Owner_IdAndEndBeforeOrderByStartDesc(eq(ownerId), any(LocalDateTime.class));

        List<BookingDto> listRejected = svc.getAllBookingsOfOwner(ownerId, "REJECTED");
        assertThat(listRejected).hasSize(1);
        assertThat(listRejected.get(0).getStatus()).isEqualTo(BookingStatus.REJECTED);
        verify(bookingRepository).findAllByItem_Owner_IdAndStatusOrderByStartDesc(ownerId, BookingStatus.REJECTED);
    }

    // ======= добавлено: respond(...) =======

    @Test
    @DisplayName("respond: владелец подтверждает WAITING → статус становится APPROVED")
    void respondApproveSuccess() {
        long ownerId = 1L;
        long bookerId = 2L;
        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");

        Item it = item(10L, "Дрель", true, owner);
        Booking bk = booking(100L, it, booker, LocalDateTime.now(), LocalDateTime.now().plusHours(1), BookingStatus.WAITING);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(bk));
        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        BookingDto out = svc.respond(100L, true, ownerId);
        assertThat(out.getStatus()).isEqualTo(BookingStatus.APPROVED);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    @DisplayName("respond: владелец отклоняет WAITING → статус становится REJECTED")
    void respondRejectSuccess() {
        long ownerId = 1L;
        long bookerId = 2L;
        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");

        Item it = item(10L, "Дрель", true, owner);
        Booking bk = booking(101L, it, booker, LocalDateTime.now(), LocalDateTime.now().plusHours(1), BookingStatus.WAITING);

        when(bookingRepository.findById(101L)).thenReturn(Optional.of(bk));
        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        BookingDto out = svc.respond(101L, false, ownerId);
        assertThat(out.getStatus()).isEqualTo(BookingStatus.REJECTED);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    @DisplayName("respond: не владелец → ForbiddenException")
    void respondNotOwnerForbidden() {
        long ownerId = 1L;
        long otherUser = 9L;
        long bookerId = 2L;

        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");

        Item it = item(10L, "Дрель", true, owner);
        Booking bk = booking(102L, it, booker, LocalDateTime.now(), LocalDateTime.now().plusHours(1), BookingStatus.WAITING);

        when(bookingRepository.findById(102L)).thenReturn(Optional.of(bk));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        assertThatThrownBy(() -> svc.respond(102L, true, otherUser))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Вы не владелец");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("respond: статус уже не WAITING → ForbiddenException")
    void respondAlreadyConfirmedForbidden() {
        long ownerId = 1L;
        long bookerId = 2L;

        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");

        Item it = item(10L, "Дрель", true, owner);
        Booking bk = booking(103L, it, booker, LocalDateTime.now(), LocalDateTime.now().plusHours(1), BookingStatus.APPROVED);

        when(bookingRepository.findById(103L)).thenReturn(Optional.of(bk));
        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        assertThatThrownBy(() -> svc.respond(103L, true, ownerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Статус заказа уже подтвержден");

        verify(bookingRepository, never()).save(any());
    }

    // ======= добавлено: getBookingByUser(...) =======

    @Test
    @DisplayName("getBookingByUser: как бронирующий получает свою бронь")
    void getBookingByUserAsBooker() {
        long ownerId = 1L;
        long bookerId = 2L;

        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");

        Item it = item(10L, "Дрель", true, owner);
        Booking bk = booking(200L, it, booker, LocalDateTime.now(), LocalDateTime.now().plusHours(1), BookingStatus.WAITING);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(bk));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        BookingDto out = svc.getBookingByUser(200L, bookerId);
        assertThat(out.getId()).isEqualTo(200L);
        assertThat(out.getBooker().getId()).isEqualTo(bookerId);
    }

    @Test
    @DisplayName("getBookingByUser: как владелец получает бронь по своей вещи")
    void getBookingByUserAsOwner() {
        long ownerId = 1L;
        long bookerId = 2L;

        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");

        Item it = item(10L, "Дрель", true, owner);
        Booking bk = booking(201L, it, booker, LocalDateTime.now(), LocalDateTime.now().plusHours(1), BookingStatus.APPROVED);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(201L)).thenReturn(Optional.of(bk));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        BookingDto out = svc.getBookingByUser(201L, ownerId);
        assertThat(out.getId()).isEqualTo(201L);
        assertThat(out.getItem().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getBookingByUser: посторонний пользователь → ForbiddenException")
    void getBookingByUserForbidden() {
        long ownerId = 1L;
        long bookerId = 2L;
        long strangerId = 99L;

        User owner = user(ownerId, "owner");
        User booker = user(bookerId, "booker");
        User stranger = user(strangerId, "str");

        Item it = item(10L, "Дрель", true, owner);
        Booking bk = booking(202L, it, booker, LocalDateTime.now(), LocalDateTime.now().plusHours(1), BookingStatus.APPROVED);

        when(userRepository.findById(strangerId)).thenReturn(Optional.of(stranger));
        when(bookingRepository.findById(202L)).thenReturn(Optional.of(bk));

        BookingServiceImpl svc = new BookingServiceImpl(
                bookingRepository, userRepository, itemRepository,
                new BookingMapper(), new ItemMapper(), new UserMapper()
        );

        assertThatThrownBy(() -> svc.getBookingByUser(202L, strangerId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Не ваш предмет");
    }
}
