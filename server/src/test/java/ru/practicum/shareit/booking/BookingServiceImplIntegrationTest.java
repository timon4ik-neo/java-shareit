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
import java.util.List;

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
    void getAllByBooker_shouldReturnBookingsForState() {
        Long ownerId = createUser("Owner", "owner7@mail.com");
        Long bookerId = createUser("Booker", "booker7@mail.com");
        ItemDto item = createItem(ownerId, true);
        bookingService.create(bookerId, futureBooking(item.getId()));

        List<BookingDto> future = bookingService.getAllByBooker(bookerId, BookingState.FUTURE);
        List<BookingDto> past = bookingService.getAllByBooker(bookerId, BookingState.PAST);

        assertThat(future).hasSize(1);
        assertThat(past).isEmpty();
    }

    @Test
    void getAllByOwner_shouldReturnBookingsForOwnerItems() {
        Long ownerId = createUser("Owner", "owner8@mail.com");
        Long bookerId = createUser("Booker", "booker8@mail.com");
        ItemDto item = createItem(ownerId, true);
        bookingService.create(bookerId, futureBooking(item.getId()));

        List<BookingDto> all = bookingService.getAllByOwner(ownerId, BookingState.ALL);

        assertThat(all).hasSize(1);
    }
}
