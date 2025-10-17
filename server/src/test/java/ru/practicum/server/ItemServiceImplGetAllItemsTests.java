package ru.practicum.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import ru.practicum.server.booking.model.Booking;
import ru.practicum.server.booking.model.BookingStatus;
import ru.practicum.server.booking.repository.BookingRepository;
import ru.practicum.server.item.dto.CommentDto;
import ru.practicum.server.item.dto.ItemDto;
import ru.practicum.server.item.mapper.CommentMapper;
import ru.practicum.server.item.mapper.ItemMapper;
import ru.practicum.server.item.model.Comment;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.item.repository.CommentRepository;
import ru.practicum.server.item.repository.ItemRepository;
import ru.practicum.server.item.service.ItemServiceImpl;
import ru.practicum.server.request.repository.ItemRequestRepository;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ItemServiceImpl.class)
@Import({ItemMapper.class, CommentMapper.class})
class ItemServiceImplGetAllItemsTests {

    @MockBean private ItemRepository itemRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private CommentRepository commentRepository;
    @MockBean private BookingRepository bookingRepository;
    @MockBean private ItemRequestRepository itemRequestRepository;

    private static User user(long id, String name) {
        return User.builder().id(id).name(name).email(name + "@ex.com").build();
    }

    private static Item item(long id, String name, boolean available, User owner) {
        return Item.builder().id(id).name(name).available(available).owner(owner).build();
    }

    private static Booking booking(long id, Item it, User booker, LocalDateTime start, LocalDateTime end, BookingStatus st) {
        return Booking.builder().id(id).item(it).booker(booker).start(start).end(end).status(st).build();
    }

    private static Comment comment(long id, Item it, User author, String text, LocalDateTime created) {
        return Comment.builder().id(id).item(it).author(author).text(text).created(created).build();
    }

    @Test
    @DisplayName("getAllItems: владельцу возвращаются вещи с комментариями и last/next (APPROVED)")
    void getAllItems_success_withBookingsAndComments() {
        long ownerId = 1L;
        User owner = user(ownerId, "owner");
        User booker = user(2L, "booker");

        Item item1 = item(10L, "Дрель", true, owner);
        Item item2 = item(11L, "Лобзик", true, owner);

        when(itemRepository.findAllByOwnerId(ownerId)).thenReturn(List.of(item1, item2));

        LocalDateTime now = LocalDateTime.now();

        Booking pastI1 = booking(100L, item1, booker, now.minusHours(5), now.minusHours(1), BookingStatus.APPROVED);
        Booking futureI1 = booking(101L, item1, booker, now.plusHours(2), now.plusHours(4), BookingStatus.APPROVED);
        Booking pastI2 = booking(200L, item2, booker, now.minusDays(1), now.minusHours(20), BookingStatus.APPROVED);
        Booking futureI2 = booking(201L, item2, booker, now.plusDays(1), now.plusDays(1).plusHours(2), BookingStatus.APPROVED);

        when(bookingRepository.findByItem_IdInAndStatusOrderByStartDesc(
                ArgumentMatchers.eq(List.of(item1.getId(), item2.getId())),
                ArgumentMatchers.eq(BookingStatus.APPROVED))
        ).thenReturn(List.of(
                futureI1, futureI2, pastI1, pastI2
        ));

        Comment i1Old = comment(1000L, item1, booker, "old-1", now.minusDays(2));
        Comment i1New = comment(1001L, item1, booker, "new-1", now.minusHours(3));
        Comment i2New = comment(2000L, item2, booker, "new-2", now.minusMinutes(10));

        when(commentRepository.findAllByItem_IdIn(List.of(item1.getId(), item2.getId())))
                .thenReturn(List.of(i1Old, i1New, i2New));

        List<ItemDto> result = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).getAllItems(ownerId);

        assertThat(result).hasSize(2);

        ItemDto dto1 = result.stream().filter(d -> d.getId().equals(item1.getId())).findFirst().orElseThrow();
        ItemDto dto2 = result.stream().filter(d -> d.getId().equals(item2.getId())).findFirst().orElseThrow();

        assertThat(dto1.getComments()).extracting(CommentDto::getText)
                .containsExactly("new-1", "old-1");

        assertThat(dto1.getLastBooking()).isNotNull();
        assertThat(dto1.getLastBooking().getId()).isEqualTo(pastI1.getId());
        assertThat(dto1.getNextBooking()).isNotNull();
        assertThat(dto1.getNextBooking().getId()).isEqualTo(futureI1.getId());

        assertThat(dto2.getComments()).extracting(CommentDto::getText)
                .containsExactly("new-2");
        assertThat(dto2.getLastBooking()).isNotNull();
        assertThat(dto2.getLastBooking().getId()).isEqualTo(pastI2.getId());
        assertThat(dto2.getNextBooking()).isNotNull();
        assertThat(dto2.getNextBooking().getId()).isEqualTo(futureI2.getId());

        verify(bookingRepository, times(1))
                .findByItem_IdInAndStatusOrderByStartDesc(List.of(item1.getId(), item2.getId()), BookingStatus.APPROVED);
        verify(commentRepository, times(1))
                .findAllByItem_IdIn(List.of(item1.getId(), item2.getId()));
    }

    @Test
    @DisplayName("getAllItems: у пользователя нет вещей - пустой список")
    void getAllItems_empty() {
        long ownerId = 42L;
        when(itemRepository.findAllByOwnerId(ownerId)).thenReturn(List.of());

        List<ItemDto> result = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).getAllItems(ownerId);

        assertThat(result).isEmpty();

        verifyNoInteractions(bookingRepository);
        verifyNoInteractions(commentRepository);
    }
}
