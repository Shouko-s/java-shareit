package ru.practicum.shareit.exception;

public class NotAvailable extends RuntimeException {
    public NotAvailable(String message) {
        super(message);
    }
}
