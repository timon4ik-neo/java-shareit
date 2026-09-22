package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.EmailAlreadyExistsException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class UserServiceImplIntegrationTest {

    @Autowired
    private UserService userService;

    private UserDto newUserDto(String name, String email) {
        return UserDto.builder().name(name).email(email).build();
    }

    @Test
    void create_shouldPersistUserAndAssignId() {
        UserDto created = userService.create(newUserDto("Alice", "alice@mail.com"));

        assertThat(created.getId()).isNotNull();
        assertEquals("Alice", created.getName());
        assertEquals("alice@mail.com", created.getEmail());
    }

    @Test
    void create_shouldThrowWhenEmailAlreadyTaken() {
        userService.create(newUserDto("Alice", "dup@mail.com"));

        assertThatThrownBy(() -> userService.create(newUserDto("Bob", "dup@mail.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void update_shouldChangeOnlyProvidedFields() {
        UserDto created = userService.create(newUserDto("Alice", "alice2@mail.com"));

        UserDto updated = userService.update(created.getId(), UserDto.builder().name("Alice Updated").build());

        assertEquals("Alice Updated", updated.getName());
        assertEquals("alice2@mail.com", updated.getEmail());
    }

    @Test
    void update_shouldKeepExistingValuesWhenNothingProvided() {
        UserDto created = userService.create(newUserDto("Alice", "alice2b@mail.com"));

        UserDto updated = userService.update(created.getId(), UserDto.builder().build());

        assertEquals("Alice", updated.getName());
        assertEquals("alice2b@mail.com", updated.getEmail());
    }

    @Test
    void update_shouldThrowWhenNewEmailBelongsToAnotherUser() {
        userService.create(newUserDto("Alice", "taken@mail.com"));
        UserDto bob = userService.create(newUserDto("Bob", "bob@mail.com"));

        assertThatThrownBy(() -> userService.update(bob.getId(), UserDto.builder().email("taken@mail.com").build()))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void getById_shouldReturnUser() {
        UserDto created = userService.create(newUserDto("Alice", "alice3@mail.com"));

        UserDto found = userService.getById(created.getId());

        assertEquals(created.getId(), found.getId());
    }

    @Test
    void getById_shouldThrowWhenUserNotFound() {
        assertThatThrownBy(() -> userService.getById(999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAll_shouldReturnAllCreatedUsers() {
        userService.create(newUserDto("Alice", "alice4@mail.com"));
        userService.create(newUserDto("Bob", "bob4@mail.com"));

        List<UserDto> all = userService.getAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void delete_shouldRemoveUser() {
        UserDto created = userService.create(newUserDto("Alice", "alice5@mail.com"));

        userService.delete(created.getId());

        assertThatThrownBy(() -> userService.getById(created.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
