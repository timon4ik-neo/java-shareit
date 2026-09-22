package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class ItemRequestServiceImplIntegrationTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    private Long createUser(String name, String email) {
        return userService.create(UserDto.builder().name(name).email(email).build()).getId();
    }

    @Test
    void create_shouldPersistRequestWithoutItems() {
        Long requestorId = createUser("Requester", "req1@mail.com");

        ItemRequestResponseDto created = itemRequestService.create(requestorId,
                ItemRequestDto.builder().description("Need a drill").build());

        assertThat(created.getId()).isNotNull();
        assertEquals("Need a drill", created.getDescription());
        assertThat(created.getItems()).isEmpty();
    }

    @Test
    void create_shouldThrowWhenRequestorNotFound() {
        assertThatThrownBy(() -> itemRequestService.create(999_999L,
                ItemRequestDto.builder().description("Need a drill").build()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwn_shouldReturnRequestsWithResponses() {
        Long requestorId = createUser("Requester", "req2@mail.com");
        Long ownerId = createUser("Owner", "owner2@mail.com");
        ItemRequestResponseDto request = itemRequestService.create(requestorId,
                ItemRequestDto.builder().description("Need a drill").build());
        ItemDto item = itemService.create(ownerId, ItemDto.builder()
                .name("Drill").description("d").available(true).requestId(request.getId()).build());

        List<ItemRequestResponseDto> own = itemRequestService.getOwn(requestorId);

        assertThat(own).hasSize(1);
        assertThat(own.get(0).getItems()).extracting("id").containsExactly(item.getId());
    }

    @Test
    void getAll_shouldExcludeOwnRequests() {
        Long requestorId = createUser("Requester", "req3@mail.com");
        Long otherId = createUser("Other", "other3@mail.com");
        itemRequestService.create(requestorId, ItemRequestDto.builder().description("Need a drill").build());

        List<ItemRequestResponseDto> allForOther = itemRequestService.getAll(otherId);
        List<ItemRequestResponseDto> allForRequestor = itemRequestService.getAll(requestorId);

        assertThat(allForOther).hasSize(1);
        assertThat(allForRequestor).isEmpty();
    }

    @Test
    void getById_shouldBeVisibleToAnyUser() {
        Long requestorId = createUser("Requester", "req4@mail.com");
        Long strangerId = createUser("Stranger", "stranger4@mail.com");
        ItemRequestResponseDto request = itemRequestService.create(requestorId,
                ItemRequestDto.builder().description("Need a drill").build());

        ItemRequestResponseDto found = itemRequestService.getById(strangerId, request.getId());

        assertEquals(request.getId(), found.getId());
    }

    @Test
    void getById_shouldThrowWhenRequestNotFound() {
        Long userId = createUser("User", "user4@mail.com");

        assertThatThrownBy(() -> itemRequestService.getById(userId, 999_999L))
                .isInstanceOf(NotFoundException.class);
    }
}
