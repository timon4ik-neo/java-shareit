package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    public ItemDto create(Long ownerId, ItemDto itemDto) {
        User owner = getUserOrThrow(ownerId);
        ItemRequest request = null;
        if (itemDto.getRequestId() != null) {
            request = itemRequestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> new NotFoundException(
                            "Запрос с id=" + itemDto.getRequestId() + " не найден"));
        }
        Item item = ItemMapper.toItem(itemDto, owner, request);
        return ItemMapper.toDto(itemRepository.save(item));
    }

    @Override
    public ItemDto update(Long ownerId, Long itemId, ItemDto itemDto) {
        Item existingItem = getItemOrThrow(itemId);

        if (!existingItem.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Пользователь с id=" + ownerId + " не является владельцем вещи с id=" + itemId);
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            existingItem.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            existingItem.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toDto(itemRepository.save(existingItem));
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        Item item = getItemOrThrow(itemId);
        List<CommentDto> comments = getComments(itemId);

        BookingShortDto lastBooking = null;
        BookingShortDto nextBooking = null;
        if (item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();
            lastBooking = bookingRepository
                    .findFirstByItem_IdAndStatusAndStartBeforeOrderByStartDesc(itemId, BookingStatus.APPROVED, now)
                    .map(ItemServiceImpl::toBookingShortDto)
                    .orElse(null);
            nextBooking = bookingRepository
                    .findFirstByItem_IdAndStatusAndStartAfterOrderByStartAsc(itemId, BookingStatus.APPROVED, now)
                    .map(ItemServiceImpl::toBookingShortDto)
                    .orElse(null);
        }

        return ItemMapper.toDto(item, lastBooking, nextBooking, comments);
    }

    @Override
    public List<ItemDto> getAllByOwner(Long ownerId) {
        List<Item> items = itemRepository.findByOwner_Id(ownerId);
        List<Long> itemIds = items.stream().map(Item::getId).toList();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, List<Booking>> bookingsByItemId = bookingRepository
                .findByItem_IdInAndStatusOrderByStartAsc(itemIds, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));

        Map<Long, List<CommentDto>> commentsByItemId = commentRepository.findByItem_IdIn(itemIds).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toDto, Collectors.toList())));

        return items.stream()
                .map(item -> {
                    List<Booking> itemBookings = bookingsByItemId.getOrDefault(item.getId(), List.of());
                    BookingShortDto lastBooking = itemBookings.stream()
                            .filter(booking -> !booking.getStart().isAfter(now))
                            .max(Comparator.comparing(Booking::getStart))
                            .map(ItemServiceImpl::toBookingShortDto)
                            .orElse(null);
                    BookingShortDto nextBooking = itemBookings.stream()
                            .filter(booking -> booking.getStart().isAfter(now))
                            .min(Comparator.comparing(Booking::getStart))
                            .map(ItemServiceImpl::toBookingShortDto)
                            .orElse(null);
                    List<CommentDto> comments = commentsByItemId.getOrDefault(item.getId(), List.of());
                    return ItemMapper.toDto(item, lastBooking, nextBooking, comments);
                })
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toDto)
                .toList();
    }

    @Override
    public CommentDto addComment(Long authorId, Long itemId, CommentRequestDto commentRequestDto) {
        Item item = getItemOrThrow(itemId);
        User author = getUserOrThrow(authorId);

        boolean hasCompletedBooking = bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndBefore(
                itemId, authorId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!hasCompletedBooking) {
            throw new ValidationException(
                    "Пользователь с id=" + authorId + " не брал вещь с id=" + itemId + " в аренду");
        }

        Comment comment = CommentMapper.toComment(commentRequestDto.getText(), item, author);
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    private List<CommentDto> getComments(Long itemId) {
        return commentRepository.findByItem_Id(itemId).stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    private static BookingShortDto toBookingShortDto(Booking booking) {
        return BookingShortDto.builder()
                .id(booking.getId())
                .bookerId(booking.getBooker().getId())
                .build();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
    }
}
