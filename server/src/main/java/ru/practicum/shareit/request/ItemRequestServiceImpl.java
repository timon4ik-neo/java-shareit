package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemRequestResponseDto create(Long requestorId, ItemRequestDto requestDto) {
        User requestor = getUserOrThrow(requestorId);
        ItemRequest itemRequest = ItemRequestMapper.toItemRequest(requestDto.getDescription(), requestor);
        ItemRequest saved = itemRequestRepository.save(itemRequest);
        return ItemRequestMapper.toDto(saved, List.of());
    }

    @Override
    public List<ItemRequestResponseDto> getOwn(Long requestorId) {
        getUserOrThrow(requestorId);
        List<ItemRequest> requests = itemRequestRepository.findByRequestor_IdOrderByCreatedDesc(requestorId);
        return toDtosWithItems(requests);
    }

    @Override
    public List<ItemRequestResponseDto> getAll(Long userId) {
        getUserOrThrow(userId);
        List<ItemRequest> requests = itemRequestRepository.findByRequestor_IdNotOrderByCreatedDesc(userId);
        return toDtosWithItems(requests);
    }

    @Override
    public ItemRequestResponseDto getById(Long userId, Long requestId) {
        getUserOrThrow(userId);
        ItemRequest itemRequest = getRequestOrThrow(requestId);
        List<Item> items = itemRepository.findByRequest_Id(requestId);
        return ItemRequestMapper.toDto(itemRequest, items);
    }

    private List<ItemRequestResponseDto> toDtosWithItems(List<ItemRequest> requests) {
        List<Long> requestIds = requests.stream().map(ItemRequest::getId).toList();
        Map<Long, List<Item>> itemsByRequestId = itemRepository.findByRequest_IdIn(requestIds).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId()));

        return requests.stream()
                .map(request -> ItemRequestMapper.toDto(request, itemsByRequestId.getOrDefault(request.getId(), List.of())))
                .toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private ItemRequest getRequestOrThrow(Long requestId) {
        return itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));
    }
}
