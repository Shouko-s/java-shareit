package ru.practicum.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import ru.practicum.server.booking.model.Booking;
import ru.practicum.server.booking.model.BookingStatus;
import ru.practicum.server.booking.repository.BookingRepository;
import ru.practicum.server.exception.ForbiddenException;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.item.dto.CommentDto;
import ru.practicum.server.item.dto.ItemDto;
import ru.practicum.server.item.mapper.CommentMapper;
import ru.practicum.server.item.mapper.ItemMapper;
import ru.practicum.server.item.model.Comment;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.item.repository.CommentRepository;
import ru.practicum.server.item.repository.ItemRepository;
import ru.practicum.server.item.service.ItemServiceImpl;
import ru.practicum.server.request.model.ItemRequest;
import ru.practicum.server.request.repository.ItemRequestRepository;
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

@SpringBootTest(classes = ItemServiceImpl.class)
@Import({ItemMapper.class, CommentMapper.class})
class ItemServiceTests {

    @MockBean
    private ItemRepository itemRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CommentRepository commentRepository;
    @MockBean
    private BookingRepository bookingRepository;
    @MockBean
    private ItemRequestRepository itemRequestRepository;

    private static User user(long id, String name) {
        return User.builder().id(id).name(name).email(name + "@ex.com").build();
    }

    private static Item item(long id, String name, boolean available, User owner) {
        return Item.builder().id(id).name(name).description("desc").available(available).owner(owner).build();
    }

    private static Comment comment(long id, Item it, User author, String text, LocalDateTime created) {
        return Comment.builder().id(id).item(it).author(author).text(text).created(created).build();
    }

    private static Booking booking(long id, Item it, User booker, LocalDateTime start, LocalDateTime end, BookingStatus st) {
        return Booking.builder().id(id).item(it).booker(booker).start(start).end(end).status(st).build();
    }

    @Test
    @DisplayName("addItem успешно сохраняет предмет без requestId")
    void addItemSuccessWithoutRequest() {
        long ownerId = 1L;
        User owner = user(ownerId, "owner");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        ItemDto dto = ItemDto.builder().name("Дрель").description("ударная").available(true).build();

        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ItemDto out = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).addItem(ownerId, dto);

        assertThat(out.getId()).isEqualTo(10L);
        assertThat(out.getName()).isEqualTo("Дрель");

        ArgumentCaptor<Item> cap = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(cap.capture());
        assertThat(cap.getValue().getOwner().getId()).isEqualTo(ownerId);
        assertThat(cap.getValue().getItemRequest()).isNull();
    }

    @Test
    @DisplayName("addItem c requestId присваивает ItemRequest")
    void addItemWithRequest() {
        long ownerId = 1L;
        long reqId = 99L;
        User owner = user(ownerId, "owner");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        ItemRequest req = ItemRequest.builder().id(reqId).requester(user(2L, "r")).build();
        when(itemRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        ItemDto dto = ItemDto.builder()
                .name("Лобзик").description("электрический").available(true).requestId(reqId).build();

        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item saved = inv.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        ItemDto out = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).addItem(ownerId, dto);

        assertThat(out.getId()).isEqualTo(11L);
        ArgumentCaptor<Item> cap = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(cap.capture());
        assertThat(cap.getValue().getItemRequest()).isNotNull();
        assertThat(cap.getValue().getItemRequest().getId()).isEqualTo(reqId);
    }

    @Test
    @DisplayName("updateItem: владелец обновляет поля")
    void updateItemSuccess() {
        long ownerId = 1L;
        User owner = user(ownerId, "owner");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        Item it = item(10L, "Old", true, owner);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(it));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemDto patch = ItemDto.builder().name("New").description("new-desc").available(false).build();

        ItemDto updated = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).updateItem(ownerId, 10L, patch);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getDescription()).isEqualTo("new-desc");
        assertThat(updated.getAvailable()).isFalse();
    }

    @Test
    @DisplayName("updateItem: не владелец получает ForbiddenException")
    void updateItemForbiddenForNotOwner() {
        long ownerId = 1L;
        long otherId = 2L;
        when(userRepository.findById(otherId)).thenReturn(Optional.of(user(otherId, "x")));

        Item it = item(10L, "Old", true, user(ownerId, "owner"));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(it));

        assertThatThrownBy(() ->
                new ItemServiceImpl(
                        itemRepository, userRepository, new ItemMapper(),
                        commentRepository, new CommentMapper(),
                        bookingRepository, itemRequestRepository
                ).updateItem(otherId, 10L, ItemDto.builder().name("N").build())
        ).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Только владелец");
    }

    @Test
    @DisplayName("getItemById: для владельца возвращаются комментарии и last/next")
    void getItemByIdForOwner() {
        long ownerId = 1L;
        long itemId = 10L;
        User owner = user(ownerId, "owner");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        Item it = item(itemId, "Дрель", true, owner);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(it));

        LocalDateTime now = LocalDateTime.now();
        User booker = user(3L, "booker");

        Booking last = booking(100L, it, booker, now.minusHours(3), now.minusHours(1), BookingStatus.APPROVED);
        Booking next = booking(101L, it, booker, now.plusHours(2), now.plusHours(4), BookingStatus.APPROVED);

        when(bookingRepository.findTop1ByItem_IdAndStartLessThanEqualOrderByStartDesc(eq(itemId), any(LocalDateTime.class)))
                .thenReturn(List.of(last));
        when(bookingRepository.findTop1ByItem_IdAndStartAfterOrderByStartAsc(eq(itemId), any(LocalDateTime.class)))
                .thenReturn(List.of(next));

        Comment c1 = comment(1L, it, booker, "new", now.minusMinutes(5));
        Comment c0 = comment(2L, it, booker, "old", now.minusDays(1));
        when(commentRepository.findAllByItem_IdOrderByCreatedDesc(itemId)).thenReturn(List.of(c1, c0));

        ItemDto dto = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).getItemById(itemId, ownerId);

        assertThat(dto.getComments()).extracting(CommentDto::getText).containsExactly("new", "old");
        assertThat(dto.getLastBooking()).isNotNull();
        assertThat(dto.getLastBooking().getId()).isEqualTo(100L);
        assertThat(dto.getNextBooking()).isNotNull();
        assertThat(dto.getNextBooking().getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("getItemById: не владелец получает комментарии, но last/next = null")
    void getItemByIdForNotOwner() {
        long requesterId = 5L;
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(user(requesterId, "req")));

        long ownerId = 1L;
        long itemId = 10L;
        Item it = item(itemId, "Дрель", true, user(ownerId, "owner"));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(it));

        when(commentRepository.findAllByItem_IdOrderByCreatedDesc(itemId))
                .thenReturn(List.of(comment(1L, it, user(9L, "u"), "c", LocalDateTime.now())));

        ItemDto dto = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).getItemById(itemId, requesterId);

        assertThat(dto.getComments()).hasSize(1);
        assertThat(dto.getLastBooking()).isNull();
        assertThat(dto.getNextBooking()).isNull();
        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("getAllItems возвращает вещи владельца с комментариями и last/next (APPROVED)")
    void getAllItemsSuccessWithBookingsAndComments() {
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
        ).thenReturn(List.of(futureI1, futureI2, pastI1, pastI2));

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

        assertThat(dto1.getComments()).extracting(CommentDto::getText).containsExactly("new-1", "old-1");
        assertThat(dto1.getLastBooking().getId()).isEqualTo(pastI1.getId());
        assertThat(dto1.getNextBooking().getId()).isEqualTo(futureI1.getId());

        assertThat(dto2.getComments()).extracting(CommentDto::getText).containsExactly("new-2");
        assertThat(dto2.getLastBooking().getId()).isEqualTo(pastI2.getId());
        assertThat(dto2.getNextBooking().getId()).isEqualTo(futureI2.getId());

        verify(bookingRepository).findByItem_IdInAndStatusOrderByStartDesc(List.of(item1.getId(), item2.getId()), BookingStatus.APPROVED);
        verify(commentRepository).findAllByItem_IdIn(List.of(item1.getId(), item2.getId()));
    }

    @Test
    @DisplayName("getAllItems у пользователя нет вещей возвращает пустой список")
    void getAllItemsEmpty() {
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

    @Test
    @DisplayName("search возвращает только доступные вещи, найденные по name/description (ignoreCase)")
    void searchSuccess() {
        String q = "дрЕль";
        User owner = user(1L, "owner");
        Item i1 = item(10L, "Дрель", true, owner);
        Item i2 = item(11L, "Супер дрель", true, owner);

        when(itemRepository.findByDescriptionContainingIgnoreCaseOrNameContainingIgnoreCaseAndAvailableTrue(eq(q), eq(q)))
                .thenReturn(List.of(i1, i2));

        List<ItemDto> res = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).search(q);

        assertThat(res).hasSize(2);
        assertThat(res).extracting(ItemDto::getId).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    @DisplayName("addComment успешно сохраняет комментарий при наличии завершённой аренды")
    void addCommentSuccess() {
        long itemId = 5L;
        long userId = 7L;
        User author = user(userId, "Alice");
        Item itm = item(itemId, "Дрель", true, user(1L, "Owner"));

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(itm));
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(bookingRepository.existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
                eq(userId), eq(itemId), eq(BookingStatus.APPROVED), any(LocalDateTime.class))
        ).thenReturn(true);

        CommentDto req = CommentDto.builder().text("Отличная вещь!").build();

        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            return Comment.builder()
                    .id(100L)
                    .item(c.getItem())
                    .author(c.getAuthor())
                    .text(c.getText())
                    .created(LocalDateTime.now())
                    .build();
        });

        CommentDto saved = new ItemServiceImpl(
                itemRepository, userRepository, new ItemMapper(),
                commentRepository, new CommentMapper(),
                bookingRepository, itemRequestRepository
        ).addComment(req, itemId, userId);

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getText()).isEqualTo("Отличная вещь!");
        assertThat(saved.getAuthorName()).isEqualTo("Alice");

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getItem().getId()).isEqualTo(itemId);
        assertThat(captor.getValue().getAuthor().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("addComment бросает ForbiddenException если нет завершённой аренды")
    void addCommentForbidden() {
        long itemId = 5L;
        long userId = 7L;
        User author = user(userId, "Alice");
        Item itm = item(itemId, "Дрель", true, user(1L, "Owner"));

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(itm));
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(bookingRepository.existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
                eq(userId), eq(itemId), eq(BookingStatus.APPROVED), any(LocalDateTime.class))
        ).thenReturn(false);

        CommentDto req = CommentDto.builder().text("Отличная вещь!").build();

        assertThatThrownBy(() ->
                new ItemServiceImpl(
                        itemRepository, userRepository, new ItemMapper(),
                        commentRepository, new CommentMapper(),
                        bookingRepository, itemRequestRepository
                ).addComment(req, itemId, userId)
        ).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Оставлять отзыв можно только после завершения аренды.");

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getItemById бросает NotFound если пользователь не найден")
    void getItemByIdUserNotFound() {
        when(userRepository.findById(777L)).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
                new ItemServiceImpl(
                        itemRepository, userRepository, new ItemMapper(),
                        commentRepository, new CommentMapper(),
                        bookingRepository, itemRequestRepository
                ).getItemById(1L, 777L)
        ).isInstanceOf(NotFoundException.class);
    }
}
