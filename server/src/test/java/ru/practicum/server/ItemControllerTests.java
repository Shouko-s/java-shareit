package ru.practicum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.server.item.controller.ItemController;
import ru.practicum.server.item.dto.CommentDto;
import ru.practicum.server.item.dto.ItemDto;
import ru.practicum.server.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemController.class)
class ItemControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    private static ItemDto item(Long id) {
        return ItemDto.builder()
                .id(id)
                .name("Дрель")
                .description("ударная")
                .available(true)
                .build();
    }

    private static CommentDto comment(Long id, String authorName, String text) {
        return CommentDto.builder()
                .id(id)
                .authorName(authorName)
                .text(text)
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /items — добавление предмета")
    void addItem() throws Exception {
        long userId = 10L;
        ItemDto req = item(null);
        ItemDto resp = item(1L);

        when(itemService.addItem(eq(userId), any(ItemDto.class))).thenReturn(resp);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1L), Long.class))
                .andExpect(jsonPath("$.name", is("Дрель")));
    }

    @Test
    @DisplayName("PATCH /items/{id} — обновление предмета")
    void updateItem() throws Exception {
        long userId = 10L;
        long itemId = 5L;
        ItemDto patch = ItemDto.builder().name("Новое имя").build();
        ItemDto resp = item(itemId);
        resp.setName("Новое имя");

        when(itemService.updateItem(eq(userId), eq(itemId), any(ItemDto.class))).thenReturn(resp);

        mockMvc.perform(patch("/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch))
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5L), Long.class))
                .andExpect(jsonPath("$.name", is("Новое имя")));
    }

    @Test
    @DisplayName("GET /items/{id} — получение предмета (с заголовком пользователя)")
    void getItemById() throws Exception {
        long userId = 7L;
        long itemId = 3L;
        ItemDto resp = item(itemId);

        when(itemService.getItemById(eq(itemId), eq(userId))).thenReturn(resp);

        mockMvc.perform(get("/items/{itemId}", itemId)
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3L), Long.class))
                .andExpect(jsonPath("$.available", is(true)));
    }

    @Test
    @DisplayName("GET /items — список предметов владельца")
    void getAllItems() throws Exception {
        long userId = 77L;
        when(itemService.getAllItems(userId)).thenReturn(List.of(item(1L), item(2L)));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1L), Long.class))
                .andExpect(jsonPath("$[1].id", is(2L), Long.class));
    }

    @Test
    @DisplayName("GET /items/search?text= — контроллер приводит текст к нижнему регистру")
    void search_lowercasesText() throws Exception {
        when(itemService.search(anyString())).thenReturn(List.of(item(10L)));

        mockMvc.perform(get("/items/search").param("text", "ДРЕЛЬ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(10L), Long.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(itemService).search(captor.capture());
        assertThat(captor.getValue()).isEqualTo("дрель");
    }

    @Test
    @DisplayName("POST /items/{id}/comment — добавить комментарий")
    void addComment() throws Exception {
        long itemId = 5L;
        long userId = 99L;
        CommentDto req = CommentDto.builder().text("Отлично!").build();
        CommentDto resp = comment(1L, "Booker", "Отлично!");

        when(itemService.addComment(any(CommentDto.class), eq(itemId), eq(userId))).thenReturn(resp);

        mockMvc.perform(post("/items/{itemId}/comment", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1L), Long.class))
                .andExpect(jsonPath("$.text", is("Отлично!")))
                .andExpect(jsonPath("$.authorName", is("Booker")));
    }
}
