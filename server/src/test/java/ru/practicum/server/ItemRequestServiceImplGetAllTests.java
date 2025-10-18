package ru.practicum.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.item.repository.ItemRepository;
import ru.practicum.server.request.dto.ItemRequestResponseDto;
import ru.practicum.server.request.mapper.ItemRequestMapper;
import ru.practicum.server.request.model.ItemRequest;
import ru.practicum.server.request.repository.ItemRequestRepository;
import ru.practicum.server.request.service.ItemRequestServiceImpl;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ItemRequestServiceImpl.class)
@Import(ItemRequestMapper.class)
class ItemRequestServiceImplGetAllTests {

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ItemRepository itemRepository;
    @MockBean
    private ItemRequestRepository itemRequestRepository;

    private static User user(long id, String name) {
        return User.builder().id(id).name(name).email(name + "@ex.com").build();
    }

    private static ItemRequest request(long id, String desc, User requester, LocalDateTime created) {
        return ItemRequest.builder().id(id).description(desc).requester(requester).created(created).build();
    }

    private static Item item(long id, String name, User owner, ItemRequest req) {
        return Item.builder().id(id).name(name).owner(owner).available(true).itemRequest(req).build();
    }

    @Test
    @DisplayName("getAll: возвращает чужие запросы, отсортированные по created DESC, с ответами items")
    void getAll_success_sortedWithAnswers() {
        long viewerId = 10L;
        User viewer = user(viewerId, "viewer");
        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        User alice = user(2L, "alice");
        User bob = user(3L, "bob");

        LocalDateTime now = LocalDateTime.now();

        ItemRequest rOld = request(100L, "старый", alice, now.minusDays(2));
        ItemRequest rNew = request(101L, "новый", bob, now.minusHours(3));

        when(itemRequestRepository.findAllByRequesterIdNot(viewerId))
                .thenReturn(new ArrayList<>(List.of(rOld, rNew)));

        Item i1 = item(1000L, "Дрель", alice, rOld);
        Item i2 = item(2000L, "Лобзик", bob, rNew);
        Item i3 = item(2001L, "Шуруповерт", bob, rNew);

        when(itemRepository.findAllByItemRequestIdIn(anyCollection()))
                .thenReturn(new ArrayList<>(List.of(i1, i2, i3)));

        List<ItemRequestResponseDto> result =
                new ItemRequestServiceImpl(userRepository, itemRepository, itemRequestRepository, new ItemRequestMapper())
                        .getAll(viewerId);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo(101L);
        assertThat(result.get(0).getDescription()).isEqualTo("новый");
        assertThat(result.get(0).getItems()).extracting("id").containsExactly(2000L, 2001L);

        assertThat(result.get(1).getId()).isEqualTo(100L);
        assertThat(result.get(1).getDescription()).isEqualTo("старый");
        assertThat(result.get(1).getItems()).extracting("id").containsExactly(1000L);

        verify(itemRequestRepository, times(1)).findAllByRequesterIdNot(viewerId);

        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(itemRepository, times(1)).findAllByItemRequestIdIn(idsCaptor.capture());
        assertThat(new HashSet<>(idsCaptor.getValue()))
                .containsExactlyInAnyOrder(100L, 101L);

        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    @DisplayName("getOwn: возвращает собственные запросы, отсортированные по created DESC, с ответами items (батч-загрузка)")
    void getOwn_success_sortedWithAnswers() {
        long userId = 42L;
        User me = user(userId, "me");
        when(userRepository.findById(userId)).thenReturn(Optional.of(me));

        LocalDateTime now = LocalDateTime.now();
        ItemRequest rOld = request(100L, "старый", me, now.minusDays(3));
        ItemRequest rNew = request(101L, "новый", me, now.minusHours(2));

        when(itemRequestRepository.findAllByRequesterId(userId))
                .thenReturn(new ArrayList<>(List.of(rOld, rNew)));

        Item i1 = item(500L, "Дрель", me, rOld);
        Item i2 = item(600L, "Лобзик", me, rNew);
        Item i3 = item(601L, "Шуруповерт", me, rNew);

        when(itemRepository.findAllByItemRequestIdIn(anyCollection()))
                .thenReturn(new ArrayList<>(List.of(i1, i2, i3)));

        List<ItemRequestResponseDto> result =
                new ItemRequestServiceImpl(userRepository, itemRepository, itemRequestRepository, new ItemRequestMapper())
                        .getOwn(userId);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo(101L);
        assertThat(result.get(0).getDescription()).isEqualTo("новый");
        assertThat(result.get(0).getItems()).extracting("id").containsExactly(600L, 601L);

        assertThat(result.get(1).getId()).isEqualTo(100L);
        assertThat(result.get(1).getDescription()).isEqualTo("старый");
        assertThat(result.get(1).getItems()).extracting("id").containsExactly(500L);

        verify(itemRequestRepository, times(1)).findAllByRequesterId(userId);

        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(itemRepository, times(1)).findAllByItemRequestIdIn(idsCaptor.capture());
        assertThat(new HashSet<>(idsCaptor.getValue()))
                .containsExactlyInAnyOrder(100L, 101L);

        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    @DisplayName("getAll: пользователь не найден → NotFoundException")
    void getAll_userNotFound() {
        when(userRepository.findById(777L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                new ItemRequestServiceImpl(userRepository, itemRepository, itemRequestRepository, new ItemRequestMapper())
                        .getAll(777L)
        )
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(itemRequestRepository);
        verifyNoInteractions(itemRepository);
    }
}
