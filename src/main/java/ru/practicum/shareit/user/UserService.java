package ru.practicum.shareit.user;

import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

/**
 * Сервис для работы с пользователями.
 */
public interface UserService {

    /**
     * Создаёт нового пользователя.
     *
     * @param userDto данные создаваемого пользователя
     * @return созданный пользователь
     */
    UserDto create(UserDto userDto);

    /**
     * Обновляет данные пользователя. Обновляются только переданные поля.
     *
     * @param userId  идентификатор пользователя
     * @param userDto новые значения полей
     * @return обновлённый пользователь
     */
    UserDto update(Long userId, UserDto userDto);

    /**
     * Возвращает пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return найденный пользователь
     */
    UserDto getById(Long userId);

    /**
     * Возвращает список всех пользователей.
     *
     * @return список пользователей
     */
    List<UserDto> getAll();

    /**
     * Удаляет пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     */
    void delete(Long userId);
}
