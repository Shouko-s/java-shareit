package ru.practicum.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.server.booking.repository.BookingRepository;
import ru.practicum.server.booking.service.BookingService;
import ru.practicum.server.booking.service.BookingServiceImpl;

@ExtendWith(MockitoExtension.class)
public class MockTest {

    @Mock
    BookingRepository bookingRepository;

    @Test
    void testCreatingBooking() {
        BookingServiceImpl bookingService;
        
    }

}
