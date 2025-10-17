package ru.practicum.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.server.booking.controller.BookingController;
import ru.practicum.server.booking.dto.BookingDto;
import ru.practicum.server.booking.dto.BookingRequest;
import ru.practicum.server.booking.model.BookingStatus;
import ru.practicum.server.booking.service.BookingService;
import ru.practicum.server.item.dto.ItemDto;
import ru.practicum.server.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private static BookingDto sampleBookingDto(long id, long itemId, long bookerId, BookingStatus status) {
        ItemDto item = ItemDto.builder()
                .id(itemId).name("Дрель").description("ударная").available(true).build();
        UserDto booker = UserDto.builder()
                .id(bookerId).name("Booker").email("booker@example.com").build();
        return BookingDto.builder()
                .id(id)
                .item(item)
                .booker(booker)
                .status(status)
                .start(LocalDateTime.now().plusHours(1))
                .end(LocalDateTime.now().plusHours(2))
                .build();
    }

    private static BookingRequest sampleRequest(long itemId) {
        return BookingRequest.builder()
                .itemId(itemId)
                .start(LocalDateTime.now().plusHours(1))
                .end(LocalDateTime.now().plusHours(2))
                .build();
    }

    @Test
    @DisplayName("POST /bookings — создаёт бронирование и возвращает DTO")
    void createBooking_returnsDto() throws Exception {
        long userId = 10L;
        BookingRequest req = sampleRequest(100L);
        BookingDto resp = sampleBookingDto(1L, 100L, userId, BookingStatus.WAITING);

        when(bookingService.addBooking(any(BookingRequest.class), eq(userId))).thenReturn(resp);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1L), Long.class))
                .andExpect(jsonPath("$.status", is("WAITING")))
                .andExpect(jsonPath("$.item.id", is(100L), Long.class))
                .andExpect(jsonPath("$.booker.id", is(10L), Long.class));
    }

    @Test
    @DisplayName("PATCH /bookings/{id}?approved=... — владелец подтверждает/отклоняет и получает DTO")
    void respond_returnsDto() throws Exception {
        long bookingId = 5L;
        long ownerId = 20L;
        boolean approved = true;

        BookingDto resp = sampleBookingDto(bookingId, 100L, 10L, BookingStatus.APPROVED);
        when(bookingService.respond(eq(bookingId), eq(approved), eq(ownerId))).thenReturn(resp);

        mockMvc.perform(patch("/bookings/{id}", bookingId)
                        .param("approved", String.valueOf(approved))
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5L), Long.class))
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    @DisplayName("GET /bookings/{id} — возвращает бронирование")
    void getBookingByBooker_returnsDto() throws Exception {
        long bookingId = 7L;
        long userId = 10L;

        BookingDto resp = sampleBookingDto(bookingId, 101L, userId, BookingStatus.WAITING);
        when(bookingService.getBookingByUser(eq(bookingId), eq(userId))).thenReturn(resp);

        mockMvc.perform(get("/bookings/{id}", bookingId)
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7L), Long.class))
                .andExpect(jsonPath("$.item.id", is(101L), Long.class))
                .andExpect(jsonPath("$.booker.id", is(10L), Long.class));
    }

    @Test
    @DisplayName("GET /bookings?state= — список бронирований пользователя")
    void getAllBookingsOfUser_returnsList() throws Exception {
        long userId = 33L;
        String state = "FUTURE";

        List<BookingDto> list = List.of(
                sampleBookingDto(1L, 100L, userId, BookingStatus.WAITING),
                sampleBookingDto(2L, 101L, userId, BookingStatus.APPROVED)
        );
        when(bookingService.getAllBookingsOfUser(eq(userId), eq(state))).thenReturn(list);

        mockMvc.perform(get("/bookings")
                        .param("state", state)
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1L), Long.class))
                .andExpect(jsonPath("$[1].status", is("APPROVED")));
    }

    @Test
    @DisplayName("GET /bookings/owner?state= — список бронирований для владельца")
    void listForOwner_returnsList() throws Exception {
        long ownerId = 44L;
        String state = "ALL";

        List<BookingDto> list = List.of(
                sampleBookingDto(10L, 200L, 77L, BookingStatus.APPROVED),
                sampleBookingDto(11L, 201L, 78L, BookingStatus.REJECTED)
        );
        when(bookingService.getAllBookingsOfOwner(eq(ownerId), eq(state))).thenReturn(list);

        mockMvc.perform(get("/bookings/owner")
                        .param("state", state)
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].item.id", is(200L), Long.class))
                .andExpect(jsonPath("$[1].status", is("REJECTED")));
    }
}
