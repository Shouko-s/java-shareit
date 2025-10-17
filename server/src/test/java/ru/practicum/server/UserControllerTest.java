package ru.practicum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.server.user.controller.UserController;
import ru.practicum.server.user.dto.UserDto;
import ru.practicum.server.user.service.UserService;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    private static UserDto user(Long id, String name, String email) {
        return UserDto.builder().id(id).name(name).email(email).build();
    }

    @Test
    @DisplayName("POST /users — создать пользователя")
    void create() throws Exception {
        UserDto req = user(null, "Bob", "b@ex.com");
        UserDto resp = user(1L, "Bob", "b@ex.com");

        when(userService.create(any(UserDto.class))).thenReturn(resp);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1L), Long.class))
                .andExpect(jsonPath("$.email", is("b@ex.com")));
    }

    @Test
    @DisplayName("PATCH /users/{id} — обновить пользователя")
    void update() throws Exception {
        long id = 5L;
        UserDto patch = user(null, "New", null);
        UserDto resp = user(id, "New", "old@ex.com");

        when(userService.updateUserById(eq(id), any(UserDto.class))).thenReturn(resp);

        mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5L), Long.class))
                .andExpect(jsonPath("$.name", is("New")));
    }

    @Test
    @DisplayName("GET /users/{id} — получить пользователя")
    void getById() throws Exception {
        long id = 7L;
        when(userService.getUserById(id)).thenReturn(user(id, "A", "a@ex.com"));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7L), Long.class))
                .andExpect(jsonPath("$.email", is("a@ex.com")));
    }

    @Test
    @DisplayName("DELETE /users/{id} — удалить пользователя")
    void deleteById() throws Exception {
        long id = 9L;
        when(userService.deleteUserById(id)).thenReturn(user(id, "Gone", "g@ex.com"));

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(9L), Long.class));
    }
}
