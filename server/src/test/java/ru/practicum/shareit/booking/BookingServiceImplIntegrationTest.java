package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class BookingServiceImplIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    private Long createUser(String name, String email) {
        return userService.create(UserDto.builder().name(name).email(email).build()).getId();
    }

    private ItemDto createItem(Long ownerId, boolean available) {
        return itemService.create(ownerId, ItemDto.builder().name("Drill").description("d").available(available).build());
    }

    private BookingRequestDto futureBooking(Long itemId) {
        return BookingRequestDto.builder()
                .itemId(itemId)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();
    }

    @Test
    void create_shouldSaveBookingInWaitingStatus() {
        Long ownerId = createUser("Owner", "owner1@mail.com");
        Long bookerId = createUser("Booker", "booker1@mail.com");
        ItemDto item = createItem(ownerId, true);

        BookingDto booking = bookingService.create(bookerId, futureBooking(item.getId()));

        assertThat(booking.getId()).isNotNull();
        assertEquals(BookingStatus.WAITING, booking.getStatus());
        assertEquals(bookerId, booking.getBooker().getId());
    }

    @Test
    void create_shouldThrowWhenItemUnavailable() {
        Long ownerId = createUser("Owner", "owner2@mail.com");
        Long bookerId = createUser("Booker", "booker2@mail.com");
        ItemDto item = createItem(ownerId, false);

        assertThatThrownBy(() -> bookingService.create(bookerId, futureBooking(item.getId())))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_shouldThrowWhenOwnerBooksOwnItem() {
        Long ownerId = createUser("Owner", "owner3@mail.com");
        ItemDto item = createItem(ownerId, true);

        assertThatThrownBy(() -> bookingService.create(ownerId, futureBooking(item.getId())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void approve_shouldSetApprovedStatusWhenCalledByOwner() {
        Long ownerId = createUser("Owner", "owner4@mail.com");
        Long bookerId = createUser("Booker", "booker4@mail.com");
        ItemDto item = createItem(ownerId, true);
        BookingDto booking = bookingService.create(bookerId, futureBooking(item.getId()));

        BookingDto approved = bookingService.approve(ownerId, booking.getId(), true);

        assertEquals(BookingStatus.APPROVED, approved.getStatus());
    }

    @Test
    void approve_shouldThrowWhenCalledByNonOwner() {
        Long ownerId = createUser("Owner", "owner5@mail.com");
        Long bookerId = createUser("Booker", "booker5@mail.com");
        ItemDto item = createItem(ownerId, true);
        BookingDto booking = bookingService.create(bookerId, futureBooking(item.getId()));

        assertThatThrownBy(() -> bookingService.approve(bookerId, booking.getId(), true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_shouldBeVisibleToBookerAndOwnerOnly() {
        Long ownerId = createUser("Owner", "owner6@mail.com");
        Long bookerId = createUser("Booker", "booker6@mail.com");
        Long strangerId = createUser("Stranger", "stranger6@mail.com");
        ItemDto item = createItem(ownerId, true);
        BookingDto booking = bookingService.create(bookerId, futureBooking(item.getId()));

        assertThat(bookingService.getById(ownerId, booking.getId()).getId()).isEqualTo(booking.getId());
        assertThat(bookingService.getById(bookerId, booking.getId()).getId()).isEqualTo(booking.getId());
        assertThatThrownBy(() -> bookingService.getById(strangerId, booking.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAllByBookerAndOwner_shouldFilterByEveryState() {
        Long ownerId = createUser("Owner", "owner7@mail.com");
        Long bookerId = createUser("Booker", "booker7@mail.com");
        LocalDateTime now = LocalDateTime.now();

        BookingDto futureWaiting = bookingService.create(bookerId,
                BookingRequestDto.builder().itemId(createItem(ownerId, true).getId())
                        .start(now.plusDays(1)).end(now.plusDays(2)).build());
        BookingDto rejected = bookingService.create(bookerId,
                BookingRequestDto.builder().itemId(createItem(ownerId, true).getId())
                        .start(now.plusDays(3)).end(now.plusDays(4)).build());
        bookingService.approve(ownerId, rejected.getId(), false);
        BookingDto current = bookingService.create(bookerId,
                BookingRequestDto.builder().itemId(createItem(ownerId, true).getId())
                        .start(now.minusHours(1)).end(now.plusHours(1)).build());
        BookingDto past = bookingService.create(bookerId,
                BookingRequestDto.builder().itemId(createItem(ownerId, true).getId())
                        .start(now.minusDays(2)).end(now.minusDays(1)).build());

        assertThat(bookingService.getAllByBooker(bookerId, BookingState.ALL)).hasSize(4);
        assertThat(bookingService.getAllByBooker(bookerId, BookingState.FUTURE))
                .extracting(BookingDto::getId).containsExactlyInAnyOrder(futureWaiting.getId(), rejected.getId());
        assertThat(bookingService.getAllByBooker(bookerId, BookingState.CURRENT))
                .extracting(BookingDto::getId).containsExactly(current.getId());
        assertThat(bookingService.getAllByBooker(bookerId, BookingState.PAST))
                .extracting(BookingDto::getId).containsExactly(past.getId());
        assertThat(bookingService.getAllByBooker(bookerId, BookingState.WAITING))
                .extracting(BookingDto::getId)
                .containsExactlyInAnyOrder(futureWaiting.getId(), current.getId(), past.getId());
        assertThat(bookingService.getAllByBooker(bookerId, BookingState.REJECTED))
                .extracting(BookingDto::getId).containsExactly(rejected.getId());

        assertThat(bookingService.getAllByOwner(ownerId, BookingState.ALL)).hasSize(4);
        assertThat(bookingService.getAllByOwner(ownerId, BookingState.FUTURE))
                .extracting(BookingDto::getId).containsExactlyInAnyOrder(futureWaiting.getId(), rejected.getId());
        assertThat(bookingService.getAllByOwner(ownerId, BookingState.CURRENT))
                .extracting(BookingDto::getId).containsExactly(current.getId());
        assertThat(bookingService.getAllByOwner(ownerId, BookingState.PAST))
                .extracting(BookingDto::getId).containsExactly(past.getId());
        assertThat(bookingService.getAllByOwner(ownerId, BookingState.WAITING))
                .extracting(BookingDto::getId)
                .containsExactlyInAnyOrder(futureWaiting.getId(), current.getId(), past.getId());
        assertThat(bookingService.getAllByOwner(ownerId, BookingState.REJECTED))
                .extracting(BookingDto::getId).containsExactly(rejected.getId());
    }
}
