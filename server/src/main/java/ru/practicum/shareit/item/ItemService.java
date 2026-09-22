package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

/**
 * Сервис для работы с вещами: создание, редактирование, поиск и отзывы.
 */
public interface ItemService {

    /**
     * Добавляет новую вещь.
     *
     * @param ownerId идентификатор владельца вещи
     * @param itemDto данные создаваемой вещи
     * @return созданная вещь
     */
    ItemDto create(Long ownerId, ItemDto itemDto);

    /**
     * Обновляет данные вещи. Доступно только владельцу, обновляются только переданные поля.
     *
     * @param ownerId идентификатор владельца вещи
     * @param itemId  идентификатор вещи
     * @param itemDto новые значения полей
     * @return обновлённая вещь
     */
    ItemDto update(Long ownerId, Long itemId, ItemDto itemDto);

    /**
     * Возвращает данные о вещи вместе с отзывами. Владельцу дополнительно показываются
     * даты последнего и ближайшего следующего бронирования.
     *
     * @param userId идентификатор запрашивающего пользователя
     * @param itemId идентификатор вещи
     * @return данные о вещи
     */
    ItemDto getById(Long userId, Long itemId);

    /**
     * Возвращает список всех вещей владельца с датами бронирований и отзывами.
     *
     * @param ownerId идентификатор владельца
     * @return список вещей владельца
     */
    List<ItemDto> getAllByOwner(Long ownerId);

    /**
     * Ищет доступные для бронирования вещи по вхождению текста в название или описание.
     *
     * @param text текст для поиска
     * @return список найденных вещей, пустой список при пустом запросе
     */
    List<ItemDto> search(String text);

    /**
     * Добавляет отзыв к вещи. Доступно только пользователю, который уже брал вещь
     * в аренду и аренда которого завершена.
     *
     * @param authorId          идентификатор автора отзыва
     * @param itemId            идентификатор вещи
     * @param commentRequestDto текст отзыва
     * @return созданный отзыв
     */
    CommentDto addComment(Long authorId, Long itemId, CommentRequestDto commentRequestDto);
}
