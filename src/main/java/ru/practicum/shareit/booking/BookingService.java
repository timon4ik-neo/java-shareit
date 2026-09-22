package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

import java.util.List;

/**
 * Сервис для работы с бронированием вещей.
 */
public interface BookingService {

    /**
     * Создаёт новый запрос на бронирование вещи в статусе {@link BookingStatus#WAITING}.
     *
     * @param bookerId   идентификатор пользователя, который бронирует вещь
     * @param requestDto данные бронирования (вещь, даты начала и окончания)
     * @return созданное бронирование
     */
    BookingDto create(Long bookerId, BookingRequestDto requestDto);

    /**
     * Подтверждает или отклоняет запрос на бронирование. Доступно только владельцу вещи.
     *
     * @param ownerId   идентификатор владельца вещи
     * @param bookingId идентификатор бронирования
     * @param approved  {@code true} — подтвердить бронирование, {@code false} — отклонить
     * @return бронирование с обновлённым статусом
     */
    BookingDto approve(Long ownerId, Long bookingId, boolean approved);

    /**
     * Возвращает данные о бронировании. Доступно автору бронирования или владельцу вещи.
     *
     * @param userId    идентификатор запрашивающего пользователя
     * @param bookingId идентификатор бронирования
     * @return найденное бронирование
     */
    BookingDto getById(Long userId, Long bookingId);

    /**
     * Возвращает список бронирований текущего пользователя, отсортированный по дате начала
     * от новых к старым.
     *
     * @param bookerId идентификатор пользователя, оформившего бронирования
     * @param state    фильтр по состоянию бронирований
     * @return список бронирований пользователя
     */
    List<BookingDto> getAllByBooker(Long bookerId, BookingState state);

    /**
     * Возвращает список бронирований для всех вещей текущего владельца, отсортированный
     * по дате начала от новых к старым.
     *
     * @param ownerId идентификатор владельца вещей
     * @param state   фильтр по состоянию бронирований
     * @return список бронирований вещей владельца
     */
    List<BookingDto> getAllByOwner(Long ownerId, BookingState state);
}
