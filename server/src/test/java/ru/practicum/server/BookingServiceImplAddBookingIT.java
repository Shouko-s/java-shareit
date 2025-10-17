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
import ru.practicum.server.booking.repository.BookingRepository;
import ru.practicum.server.booking.service.BookingServiceImpl;
import ru.practicum.server.exception.NotAvailable;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.item.mapper.ItemMapper;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.item.repository.ItemRepository;
import ru.practicum.server.user.mapper.UserMapper;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = BookingServiceImpl.class)
@Import({BookingMapper.class, ItemMapper.class, UserMapper.class})
class BookingServiceImplAddBookingIT {

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

    private static BookingRequest req(long itemId) {
        return BookingRequest.builder()
                .itemId(itemId)
                .start(LocalDateTime.now().plusHours(1))
                .end(LocalDateTime.now().plusHours(2))
                .build();
    }

    @Test
    @DisplayName("addBooking: успех при доступной вещи (двойной save)")
    void addBooking_success() {
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
    void addBooking_itemNotAvailable() {
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
    void addBooking_notFoundBranches() {
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
}
