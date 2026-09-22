package ru.practicum.shareit.request.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestResponseDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestResponseDto> json;

    @Test
    void serialize_shouldWriteCreatedDateAndItemsList() throws Exception {
        ItemRequestResponseDto dto = ItemRequestResponseDto.builder()
                .id(1L)
                .description("Need a drill")
                .created(LocalDateTime.of(2026, 9, 22, 10, 0, 0))
                .items(List.of(ItemForRequestDto.builder().id(5L).name("Drill").ownerId(7L).build()))
                .build();

        var result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Need a drill");
        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2026-09-22T10:00:00");
        assertThat(result).extractingJsonPathArrayValue("$.items").hasSize(1);
        assertThat(result).extractingJsonPathNumberValue("$.items[0].id").isEqualTo(5);
        assertThat(result).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(7);
    }

    @Test
    void serialize_shouldWriteEmptyItemsListWhenNoResponses() throws Exception {
        ItemRequestResponseDto dto = ItemRequestResponseDto.builder()
                .id(1L)
                .description("Need a drill")
                .created(LocalDateTime.now())
                .items(List.of())
                .build();

        var result = json.write(dto);

        assertThat(result).extractingJsonPathArrayValue("$.items").isEmpty();
    }
}
