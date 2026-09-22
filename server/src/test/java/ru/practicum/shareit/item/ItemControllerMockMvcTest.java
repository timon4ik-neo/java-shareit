package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemController.class)
class ItemControllerMockMvcTest {

    private static final String HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void create_shouldReturnCreatedItem() throws Exception {
        ItemDto request = ItemDto.builder().name("Drill").description("d").available(true).build();
        ItemDto response = ItemDto.builder().id(1L).name("Drill").description("d").available(true).build();
        when(itemService.create(anyLong(), any())).thenReturn(response);

        mvc.perform(post("/items")
                        .header(HEADER, 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Drill"));
    }

    @Test
    void update_shouldReturnUpdatedItem() throws Exception {
        ItemDto response = ItemDto.builder().id(1L).name("Drill").description("d").available(false).build();
        when(itemService.update(anyLong(), anyLong(), any())).thenReturn(response);

        mvc.perform(patch("/items/1")
                        .header(HEADER, 1L)
                        .contentType("application/json")
                        .content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void update_shouldReturn403WhenNotOwner() throws Exception {
        when(itemService.update(anyLong(), anyLong(), any()))
                .thenThrow(new ForbiddenException("not owner"));

        mvc.perform(patch("/items/1")
                        .header(HEADER, 2L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturnItem() throws Exception {
        when(itemService.getById(1L, 1L)).thenReturn(ItemDto.builder().id(1L).name("Drill").build());

        mvc.perform(get("/items/1").header(HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAllByOwner_shouldReturnList() throws Exception {
        when(itemService.getAllByOwner(1L)).thenReturn(List.of(
                ItemDto.builder().id(1L).name("Drill").build(),
                ItemDto.builder().id(2L).name("Ladder").build()
        ));

        mvc.perform(get("/items").header(HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void search_shouldReturnMatchingItems() throws Exception {
        when(itemService.search(anyString())).thenReturn(List.of(ItemDto.builder().id(1L).name("Drill").build()));

        mvc.perform(get("/items/search").param("text", "drill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void addComment_shouldReturnCreatedComment() throws Exception {
        when(itemService.addComment(anyLong(), anyLong(), any()))
                .thenReturn(CommentDto.builder().id(1L).text("Great!").authorName("Booker").build());

        mvc.perform(post("/items/1/comment")
                        .header(HEADER, 2L)
                        .contentType("application/json")
                        .content("{\"text\":\"Great!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Great!"));
    }
}
