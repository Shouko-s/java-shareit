package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByBooker_IdAndItem_IdAndStatusAndEndBefore(Long bookerId, Long itemId, BookingStatus status, LocalDateTime endBefore);

    List<Booking> findAllByBookerId(Long userId);

    List<Booking> findAllByItem_Owner_Id(Long itemOwnerId);

    List<Booking> findTop1ByItem_IdAndStartLessThanEqualOrderByStartDesc(Long itemId, LocalDateTime now);

    List<Booking> findTop1ByItem_IdAndStartAfterOrderByStartAsc(Long itemId, LocalDateTime now);
}
