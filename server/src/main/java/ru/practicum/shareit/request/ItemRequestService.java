package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.util.List;

/**
 * Сервис для работы с запросами вещей.
 */
public interface ItemRequestService {

    /**
     * Создаёт новый запрос вещи.
     *
     * @param requestorId идентификатор пользователя, создающего запрос
     * @param requestDto  текст запроса
     * @return созданный запрос
     */
    ItemRequestResponseDto create(Long requestorId, ItemRequestDto requestDto);

    /**
     * Возвращает список запросов текущего пользователя вместе с ответами на них,
     * отсортированный по дате создания от новых к старым.
     *
     * @param requestorId идентификатор пользователя
     * @return список собственных запросов
     */
    List<ItemRequestResponseDto> getOwn(Long requestorId);

    /**
     * Возвращает список запросов, созданных другими пользователями, отсортированный
     * по дате создания от новых к старым.
     *
     * @param userId идентификатор запрашивающего пользователя
     * @return список чужих запросов
     */
    List<ItemRequestResponseDto> getAll(Long userId);

    /**
     * Возвращает данные об одном запросе вместе с ответами на него. Доступно любому пользователю.
     *
     * @param userId    идентификатор запрашивающего пользователя
     * @param requestId идентификатор запроса
     * @return найденный запрос
     */
    ItemRequestResponseDto getById(Long userId, Long requestId);
}
