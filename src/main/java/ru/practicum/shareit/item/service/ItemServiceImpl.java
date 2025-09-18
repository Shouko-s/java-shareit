package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
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
public class ItemServiceImpl implements ItemService {
    private final ItemRepo itemRepo;
    private final UserRepo userRepo;
    private final ItemMapper itemMapper = new ItemMapper();

    @Override
    public Item addItem(long ownerId, ItemDto dto) {
        User user = getUserOrThrow(ownerId);
        Item item = itemMapper.dtoToItem(dto, user, null);
        return itemRepo.addItem(item);
    }

    @Override
    public Item updateItem(Long userId, Long itemId, ItemDto itemDto) {

        getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        String name = itemDto.getName();
        String description = itemDto.getDescription();
        Boolean available = itemDto.getAvailable();

        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Только владелец имеет доступ");
        }

        if (name != null) item.setName(name);
        if (description != null) item.setDescription(description);
        if (available != null) item.setAvailable(available);

        return itemRepo.updateItem(item);
    }

    @Override
    public Item getItemById(Long id) {
        return getItemOrThrow(id);
    }

    @Override
    public List<Item> getAllItems(Long usrId) {
        return itemRepo.getAllItems(usrId);
    }

    @Override
    public List<Item> search(String text) {
        if (text.isBlank()) return List.of();
        return itemRepo.search(text);
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
