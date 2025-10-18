package ru.practicum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.server.exception.*;
import ru.practicum.server.item.controller.ItemController;
import ru.practicum.server.item.dto.CommentDto;
import ru.practicum.server.item.dto.ItemDto;
import ru.practicum.server.item.service.ItemService;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemController.class)
@Import(ErrorHandler.class)
class ErrorHandlerMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    @DisplayName("AlreadyExists -> 500 и корректный ErrorResponse")
    void alreadyExistsReturns500() throws Exception {
        long userId = 1L;
        when(itemService.addItem(eq(userId), any(ItemDto.class)))
                .thenThrow(new AlreadyExists("Такой предмет уже существует"));
        ItemDto body = ItemDto.builder().name("Дрель").description("ударная").available(true).build();

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Уже существует")))
                .andExpect(jsonPath("$.description", is("Такой предмет уже существует")));
    }

    @Test
    @DisplayName("NotFoundException -> 404 и корректный ErrorResponse")
    void notFoundReturns404() throws Exception {
        long userId = 10L;
        long itemId = 777L;
        when(itemService.getItemById(eq(itemId), eq(userId)))
                .thenThrow(new NotFoundException("Вещь с id=777 не найдена"));

        mockMvc.perform(get("/items/{id}", itemId)
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Не найден")))
                .andExpect(jsonPath("$.description", is("Вещь с id=777 не найдена")));
    }

    @Test
    @DisplayName("ForbiddenException -> 400 и корректный ErrorResponse")
    void forbiddenReturns400() throws Exception {
        long itemId = 5L;
        long userId = 20L;
        when(itemService.addComment(any(CommentDto.class), eq(itemId), eq(userId)))
                .thenThrow(new ForbiddenException("Оставлять отзыв можно только после завершения аренды."));
        CommentDto body = CommentDto.builder().text("Текст").build();

        mockMvc.perform(post("/items/{id}/comment", itemId)
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Запрещено")))
                .andExpect(jsonPath("$.description", is("Оставлять отзыв можно только после завершения аренды.")));
    }

    @Test
    @DisplayName("NotAvailable -> 400 и корректный ErrorResponse")
    void notAvailableReturns400() throws Exception {
        long userId = 1L;
        when(itemService.addItem(eq(userId), any(ItemDto.class)))
                .thenThrow(new NotAvailable("Недоступная вещь"));
        ItemDto body = ItemDto.builder().name("Перфоратор").description("тяжёлый").available(false).build();

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Уже занято")))
                .andExpect(jsonPath("$.description", is("Недоступная вещь")));
    }
}
