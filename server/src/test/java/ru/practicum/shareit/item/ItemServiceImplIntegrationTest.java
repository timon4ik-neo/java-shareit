package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class ItemServiceImplIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    private Long createUser(String name, String email) {
        return userService.create(UserDto.builder().name(name).email(email).build()).getId();
    }

    private ItemDto newItemDto(String name, String description, boolean available) {
        return ItemDto.builder().name(name).description(description).available(available).build();
    }

    @Test
    void create_shouldPersistItemForOwner() {
        Long ownerId = createUser("Owner", "owner1@mail.com");

        ItemDto created = itemService.create(ownerId, newItemDto("Drill", "Powerful drill", true));

        assertThat(created.getId()).isNotNull();
        assertEquals("Drill", created.getName());
    }

    @Test
    void create_shouldThrowWhenOwnerNotFound() {
        assertThatThrownBy(() -> itemService.create(999_999L, newItemDto("Drill", "d", true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_shouldChangeOnlyProvidedFieldsForOwner() {
        Long ownerId = createUser("Owner", "owner2@mail.com");
        ItemDto created = itemService.create(ownerId, newItemDto("Drill", "Powerful drill", true));

        ItemDto updated = itemService.update(ownerId, created.getId(), ItemDto.builder().available(false).build());

        assertEquals("Drill", updated.getName());
        assertEquals(false, updated.getAvailable());
    }

    @Test
    void update_shouldThrowWhenNotOwner() {
        Long ownerId = createUser("Owner", "owner3@mail.com");
        Long strangerId = createUser("Stranger", "stranger3@mail.com");
        ItemDto created = itemService.create(ownerId, newItemDto("Drill", "d", true));

        assertThatThrownBy(() -> itemService.update(strangerId, created.getId(), ItemDto.builder().name("Hack").build()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_shouldReturnItemWithEmptyCommentsForNewItem() {
        Long ownerId = createUser("Owner", "owner4@mail.com");
        ItemDto created = itemService.create(ownerId, newItemDto("Drill", "d", true));

        ItemDto found = itemService.getById(ownerId, created.getId());

        assertEquals(created.getId(), found.getId());
        assertThat(found.getComments()).isEmpty();
    }

    @Test
    void getAllByOwner_shouldReturnOnlyOwnerItems() {
        Long ownerId = createUser("Owner", "owner5@mail.com");
        Long otherOwnerId = createUser("Other", "other5@mail.com");
        itemService.create(ownerId, newItemDto("Drill", "d", true));
        itemService.create(ownerId, newItemDto("Ladder", "l", true));
        itemService.create(otherOwnerId, newItemDto("Hammer", "h", true));

        List<ItemDto> items = itemService.getAllByOwner(ownerId);

        assertThat(items).hasSize(2);
    }

    @Test
    void search_shouldFindOnlyAvailableItemsMatchingText() {
        Long ownerId = createUser("Owner", "owner6@mail.com");
        ItemDto available = itemService.create(ownerId, newItemDto("SuperDrill2000", "d", true));
        itemService.create(ownerId, newItemDto("SuperDrill3000", "d", false));

        List<ItemDto> found = itemService.search("superdrill2000");

        assertThat(found).extracting(ItemDto::getId).containsExactly(available.getId());
    }

    @Test
    void search_shouldReturnEmptyListForBlankQuery() {
        assertThat(itemService.search(" ")).isEmpty();
    }

    @Test
    void addComment_shouldSucceedAfterCompletedBooking() {
        Long ownerId = createUser("Owner", "owner7@mail.com");
        Long bookerId = createUser("Booker", "booker7@mail.com");
        ItemDto item = itemService.create(ownerId, newItemDto("Drill", "d", true));

        var booking = bookingService.create(bookerId, BookingRequestDto.builder()
                .itemId(item.getId())
                .start(LocalDateTime.now().minusDays(2))
                .end(LocalDateTime.now().minusDays(1))
                .build());
        bookingService.approve(ownerId, booking.getId(), true);

        CommentDto comment = itemService.addComment(bookerId, item.getId(),
                CommentRequestDto.builder().text("Great tool!").build());

        assertEquals("Great tool!", comment.getText());
        assertEquals("Booker", comment.getAuthorName());
    }

    @Test
    void addComment_shouldThrowWhenNoCompletedBooking() {
        Long ownerId = createUser("Owner", "owner8@mail.com");
        Long strangerId = createUser("Stranger", "stranger8@mail.com");
        ItemDto item = itemService.create(ownerId, newItemDto("Drill", "d", true));

        assertThatThrownBy(() -> itemService.addComment(strangerId, item.getId(),
                CommentRequestDto.builder().text("Never rented").build()))
                .isInstanceOf(ValidationException.class);
    }
}
