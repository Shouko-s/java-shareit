package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.BookingShort;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemMapper mapper;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    @Override
    public ItemDto addItem(long ownerId, ItemDto dto) {
        User user = getUserOrThrow(ownerId);
        Item item = mapper.dtoToItem(dto, user);
        item.setOwner(user);
        log.info("Предмет добавлен item={}", item);
        return mapper.itemToDto(itemRepository.save(item));
    }

    @Override
    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) {

        String name = itemDto.getName();
        String description = itemDto.getDescription();
        Boolean available = itemDto.getAvailable();

        if ((name == null || name.isBlank())
                && (description == null || description.isBlank())
                && (available == null)) {
            throw new IllegalArgumentException("Требуется минимум один аргумент");
        }

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
        log.info("Получен предмет по id={}", itemId);
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

        return dto;
    }


    @Override
    public List<ItemDto> getAllItems(Long userId) {
        log.info("Получен список всех предметов пользователя по id={}", userId);
        return itemRepository.findAllByOwnerId(userId).stream()
                .map(mapper::itemToDto).toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) return List.of();
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

        Comment comment = Comment.builder()
                .text(commentDto.getText())
                .item(item)
                .author(user)
                .build();

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
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));
    }
}
