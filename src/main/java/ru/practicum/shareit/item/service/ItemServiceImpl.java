package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepo;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {
    private final ItemRepo itemRepo;
    private final UserRepo userRepo;
    private final ItemMapper mapper;

    @Override
    public ItemDto addItem(long ownerId, ItemDto dto) {
        User user = getUserOrThrow(ownerId);
        Item item = mapper.dtoToItem(dto, user);
        log.info("Предмет добавлен item={}", item.toString());
        return mapper.itemToDto(itemRepo.addItem(item));
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

        if (name != null) item.setName(name);
        if (description != null) item.setDescription(description);
        if (available != null) item.setAvailable(available);

        log.info("Предмет обновлен id={}", itemId);
        return mapper.itemToDto(itemRepo.updateItem(item));
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        log.info("Получен предмет по id={}", itemId);
        return mapper.itemToDto(getItemOrThrow(itemId));
    }

    @Override
    public List<ItemDto> getAllItems(Long userId) {
        log.info("Получен список всех предметов пользователя по id={}", userId);
        return itemRepo.getAllItemsOfUser(userId).stream()
                .map(mapper::itemToDto).toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) return List.of();
        log.info("Поиск по тексту text={}", text);
        return itemRepo.search(text).stream()
                .map(mapper::itemToDto).toList();
    }


    private User getUserOrThrow(long id) {
        return userRepo.getUserById(id).orElseThrow(
                () -> new NotFoundException("Пользователь с id=" + id + " не найден")
        );
    }

    private Item getItemOrThrow(long id) {
        return itemRepo.getItemById(id)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));
    }
}
