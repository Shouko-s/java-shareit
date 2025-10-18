package ru.practicum.server.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.server.booking.model.Booking;
import ru.practicum.server.booking.model.BookingStatus;
import ru.practicum.server.booking.repository.BookingRepository;
import ru.practicum.server.exception.ForbiddenException;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.item.dto.BookingShort;
import ru.practicum.server.item.dto.CommentDto;
import ru.practicum.server.item.dto.ItemDto;
import ru.practicum.server.item.mapper.CommentMapper;
import ru.practicum.server.item.mapper.ItemMapper;
import ru.practicum.server.item.model.Comment;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.item.repository.CommentRepository;
import ru.practicum.server.item.repository.ItemRepository;
import ru.practicum.server.request.model.ItemRequest;
import ru.practicum.server.request.repository.ItemRequestRepository;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemMapper mapper;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final BookingRepository bookingRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    public ItemDto addItem(long ownerId, ItemDto dto) {
        User user = getUserOrThrow(ownerId);
        Item item = mapper.dtoToItem(dto, user);
        if (dto.getRequestId() != null) {
            item.setItemRequest(getItemRequestOrThrow(dto.getRequestId()));
        }
        log.info("Предмет добавлен item={}", item);
        return mapper.itemToDto(itemRepository.save(item));
    }

    @Override
    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) {

        String name = itemDto.getName();
        String description = itemDto.getDescription();
        Boolean available = itemDto.getAvailable();

        getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Только владелец имеет доступ");
        }

        if (name != null && !name.isBlank()) item.setName(name);
        if (description != null && !description.isBlank()) item.setDescription(description);
        if (available != null) item.setAvailable(available);

        log.info("Предмет обновлен id={}", itemId);
        return mapper.itemToDto(itemRepository.save(item));
    }

    @Override
    public ItemDto getItemById(Long itemId, Long requesterId) {
        getUserOrThrow(requesterId);

        Item item = getItemOrThrow(itemId);
        ItemDto dto = mapper.itemToDto(item);

        List<CommentDto> comments = commentRepository
                .findAllByItem_IdOrderByCreatedDesc(itemId)
                .stream()
                .map(commentMapper::buildDto)
                .toList();
        dto.setComments(comments);

        if (item.getOwner().getId().equals(requesterId)) {
            LocalDateTime now = LocalDateTime.now();

            bookingRepository.findTop1ByItem_IdAndStartLessThanEqualOrderByStartDesc(itemId, now)
                    .stream()
                    .findFirst()
                    .ifPresent(b -> dto.setLastBooking(
                            BookingShort.builder().id(b.getId()).bookerId(b.getBooker().getId()).build()
                    ));

            bookingRepository.findTop1ByItem_IdAndStartAfterOrderByStartAsc(itemId, now)
                    .stream()
                    .findFirst()
                    .ifPresent(b -> dto.setNextBooking(
                            BookingShort.builder().id(b.getId()).bookerId(b.getBooker().getId()).build()
                    ));
        } else {
            dto.setLastBooking(null);
            dto.setNextBooking(null);
        }

        log.info("Получен предмет по id={}", itemId);
        return dto;
    }


    @Override
    public List<ItemDto> getAllItems(Long userId) {

        List<Item> items = itemRepository.findAllByOwnerId(userId);
        if (items.isEmpty()) return List.of();

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        LocalDateTime now = LocalDateTime.now();

        List<Booking> approved = bookingRepository.findByItem_IdInAndStatusOrderByStartDesc(itemIds, BookingStatus.APPROVED);
        Map<Long, List<Booking>> bookingsByItem = new HashMap<>();
        for (Booking b : approved) {
            bookingsByItem.computeIfAbsent(b.getItem().getId(), k -> new ArrayList<>()).add(b);
        }

        List<Comment> allComments = commentRepository.findAllByItem_IdIn(itemIds);
        Map<Long, List<CommentDto>> commentsByItem = new HashMap<>();
        for (Comment comment : allComments) {
            commentsByItem.computeIfAbsent(comment.getItem().getId(), k -> new ArrayList<>())
                    .add(commentMapper.buildDto(comment));
        }
        commentsByItem.values().forEach(list ->
                list.sort(Comparator.comparing(CommentDto::getCreated).reversed())
        );

        List<ItemDto> result = new ArrayList<>();
        for (Item item : items) {
            ItemDto dto = mapper.itemToDto(item);
            dto.setComments(commentsByItem.getOrDefault(item.getId(), List.of()));

            if (item.getOwner().getId().equals(userId)) {
                List<Booking> list = bookingsByItem
                        .getOrDefault(item.getId(), List.of());

                BookingShort last = null;
                for (Booking b : list) {
                    if (!b.getStart().isAfter(now)) {
                        last = BookingShort.builder()
                                .id(b.getId())
                                .bookerId(b.getBooker().getId())
                                .build();
                        break;
                    }
                }

                BookingShort next = null;
                for (int i = list.size() - 1; i >= 0; i--) {
                    var b = list.get(i);
                    if (b.getStart().isAfter(now)) {
                        next = BookingShort.builder()
                                .id(b.getId())
                                .bookerId(b.getBooker().getId())
                                .build();
                        break;
                    }
                }

                dto.setLastBooking(last);
                dto.setNextBooking(next);
            } else {
                dto.setLastBooking(null);
                dto.setNextBooking(null);
            }

            result.add(dto);
        }

        log.info("Получен список всех предметов пользователя по id={}", userId);
        return result;
    }

    @Override
    public List<ItemDto> search(String text) {
        log.info("Поиск по тексту text={}", text);
        return itemRepository.findByDescriptionContainingIgnoreCaseOrNameContainingIgnoreCaseAndAvailableTrue(text, text).stream()
                .map(mapper::itemToDto).toList();
    }

    @Override
    public CommentDto addComment(CommentDto commentDto, Long itemId, Long userId) {
        Item item = getItemOrThrow(itemId);
        User user = getUserOrThrow(userId);

        boolean canComment = bookingRepository
                .existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
                        userId,
                        itemId,
                        BookingStatus.APPROVED,
                        LocalDateTime.now()
                );

        if (!canComment) {
            throw new ForbiddenException("Оставлять отзыв можно только после завершения аренды.");
        }

        Comment comment = commentMapper.buildEntity(commentDto, item, user);

        Comment saved = commentRepository.save(comment);
        return commentMapper.buildDto(saved);
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

    private ItemRequest getItemRequestOrThrow(Long id) {
        return itemRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Запрос на вещь с id=" + id + "не найдена"));
    }
}
