package ru.practicum.server.exception;

public class NotAvailable extends RuntimeException {
    public NotAvailable(String message) {
        super(message);
    }
}
