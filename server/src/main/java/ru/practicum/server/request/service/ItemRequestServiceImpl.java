package ru.practicum.server.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.item.model.Item;
import ru.practicum.server.item.repository.ItemRepository;
import ru.practicum.server.request.dto.ItemRequestDto;
import ru.practicum.server.request.dto.ItemRequestResponseDto;
import ru.practicum.server.request.mapper.ItemRequestMapper;
import ru.practicum.server.request.model.ItemRequest;
import ru.practicum.server.request.repository.ItemRequestRepository;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemRequestRepository itemRequestRepository;
    private final ItemRequestMapper mapper;

    @Override
    public ItemRequestResponseDto create(Long userId, ItemRequestDto dto) {
        User requester = getUserOrThrow(userId);
        ItemRequest saved = itemRequestRepository.save(mapper.buildEntity(dto, requester));
        return mapper.buildDto(saved, List.of());
    }

    @Override
    public List<ItemRequestResponseDto> getOwn(Long userId) {
        getUserOrThrow(userId);
        List<ItemRequest> list = itemRequestRepository.findAllByRequesterId(userId);
        list.sort(Comparator.comparing(ItemRequest::getCreated).reversed());
        return list.stream()
                .map(r -> mapper.buildDto(r, itemRepository.findAllByItemRequestId(r.getId())))
                .toList();
    }

    @Override
    public List<ItemRequestResponseDto> getAll(Long userId) {
        getUserOrThrow(userId);
        List<ItemRequest> list = itemRequestRepository.findAllByRequesterIdNot(userId);
        list.sort(Comparator.comparing(ItemRequest::getCreated).reversed());
        return list.stream()
                .map(r -> mapper.buildDto(r, itemRepository.findAllByItemRequestId(r.getId())))
                .toList();
    }

    @Override
    public ItemRequestResponseDto getById(Long userId, Long requestId) {
        getUserOrThrow(userId);
        ItemRequest request = getItemRequestOrThrow(requestId);
        List<Item> answers = itemRepository.findAllByItemRequestId(requestId);
        return mapper.buildDto(request, answers);
    }

    private User getUserOrThrow(long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Пользователь с id=" + id + " не найден")
        );
    }

    private ItemRequest getItemRequestOrThrow(long id) {
        return itemRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + id + " не найдена"));
    }
}
