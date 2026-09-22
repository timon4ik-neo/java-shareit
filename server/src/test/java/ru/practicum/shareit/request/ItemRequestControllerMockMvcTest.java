package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemRequestController.class)
class ItemRequestControllerMockMvcTest {

    private static final String HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestService itemRequestService;

    private ItemRequestResponseDto sampleResponse() {
        return ItemRequestResponseDto.builder()
                .id(1L)
                .description("Need a drill")
                .created(LocalDateTime.now())
                .items(List.of())
                .build();
    }

    @Test
    void create_shouldReturnCreatedRequest() throws Exception {
        when(itemRequestService.create(anyLong(), any())).thenReturn(sampleResponse());

        mvc.perform(post("/requests")
                        .header(HEADER, 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                ItemRequestDto.builder().description("Need a drill").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Need a drill"));
    }

    @Test
    void getOwn_shouldReturnOwnRequests() throws Exception {
        when(itemRequestService.getOwn(anyLong())).thenReturn(List.of(sampleResponse()));

        mvc.perform(get("/requests").header(HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAll_shouldReturnOtherUsersRequests() throws Exception {
        when(itemRequestService.getAll(anyLong())).thenReturn(List.of(sampleResponse()));

        mvc.perform(get("/requests/all").header(HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getById_shouldReturnRequest() throws Exception {
        when(itemRequestService.getById(anyLong(), anyLong())).thenReturn(sampleResponse());

        mvc.perform(get("/requests/1").header(HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
