package ru.practicum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.server.request.controller.ItemRequestController;
import ru.practicum.server.request.dto.ItemRequestDto;
import ru.practicum.server.request.dto.ItemRequestResponseDto;
import ru.practicum.server.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ItemRequestService service;

    private static ItemRequestDto req(String d) {
        return ItemRequestDto.builder().description(d).build();
    }

    private static ItemRequestResponseDto resp(long id, String d) {
        return ItemRequestResponseDto.builder()
                .id(id).description(d).created(LocalDateTime.now())
                .items(List.of())
                .build();
    }

    @Test
    @DisplayName("POST /requests — создать запрос")
    void create() throws Exception {
        long userId = 1L;
        ItemRequestDto body = req("нужна дрель");
        ItemRequestResponseDto out = resp(100L, "нужна дрель");

        when(service.create(eq(userId), any(ItemRequestDto.class))).thenReturn(out);

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100L), Long.class))
                .andExpect(jsonPath("$.description", is("нужна дрель")));
    }

    @Test
    @DisplayName("GET /requests — получить свои запросы")
    void getOwn() throws Exception {
        long userId = 7L;
        when(service.getOwn(userId)).thenReturn(List.of(resp(1L, "a"), resp(2L, "b")));

        mockMvc.perform(get("/requests").header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1L), Long.class));
    }

    @Test
    @DisplayName("GET /requests/all — получить запросы других пользователей")
    void getAll() throws Exception {
        long userId = 7L;
        when(service.getAll(userId)).thenReturn(List.of(resp(10L, "x")));

        mockMvc.perform(get("/requests/all").header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(10L), Long.class));
    }

    @Test
    @DisplayName("GET /requests/{id} — получить конкретный запрос")
    void getById() throws Exception {
        long userId = 7L;
        long reqId = 77L;
        when(service.getById(userId, reqId)).thenReturn(resp(reqId, "z"));

        mockMvc.perform(get("/requests/{id}", reqId).header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(77L), Long.class));
    }
}
