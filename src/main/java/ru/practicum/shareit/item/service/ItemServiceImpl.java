package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
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
    public ItemDto getItemById(Long itemId) {
        log.info("Получен предмет по id={}", itemId);
        return mapper.itemToDto(getItemOrThrow(itemId));
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
        Comment comment = Comment.builder()
                .text(commentDto.getText())
                .item(item)
                .author(user)
                .build();

        Comment saved = commentRepository.save(comment);
        return commentMapper.commentToDto(saved);
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

    private BookingDto wasItemBookedByUser(long userId, long itemId) {
        return bookingService.getAllBookingsOfUser(userId).stream()
                .filter(bd -> bd.getItem().getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Вы не бронировали эту вещь"));
    }
}
