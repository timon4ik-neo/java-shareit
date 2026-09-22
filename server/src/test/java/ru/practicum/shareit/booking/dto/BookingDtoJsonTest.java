package ru.practicum.shareit.booking.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.BookingStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {

    @Autowired
    private JacksonTester<BookingDto> json;

    @Test
    void serialize_shouldWriteIsoDatesAndNestedObjects() throws Exception {
        BookingDto dto = BookingDto.builder()
                .id(1L)
                .start(LocalDateTime.of(2026, 10, 1, 10, 0, 0))
                .end(LocalDateTime.of(2026, 10, 2, 10, 0, 0))
                .status(BookingStatus.APPROVED)
                .booker(UserShortDto.builder().id(2L).build())
                .item(ItemShortDto.builder().id(3L).name("Drill").build())
                .build();

        var result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2026-10-01T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2026-10-02T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("APPROVED");
        assertThat(result).extractingJsonPathNumberValue("$.booker.id").isEqualTo(2);
        assertThat(result).extractingJsonPathNumberValue("$.item.id").isEqualTo(3);
        assertThat(result).extractingJsonPathStringValue("$.item.name").isEqualTo("Drill");
    }

    @Test
    void deserialize_shouldParseIsoDatesAndStatus() throws Exception {
        String content = "{\"id\": 1, \"start\": \"2026-10-01T10:00:00\", \"end\": \"2026-10-02T10:00:00\", "
                + "\"status\": \"WAITING\", \"booker\": {\"id\": 2}, \"item\": {\"id\": 3, \"name\": \"Drill\"}}";

        BookingDto dto = json.parseObject(content);

        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 0, 0));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2026, 10, 2, 10, 0, 0));
        assertThat(dto.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(dto.getBooker().getId()).isEqualTo(2L);
    }
}
