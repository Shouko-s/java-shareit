package ru.practicum.shareit.exception;

public class AlreadyExists extends RuntimeException {
    public AlreadyExists(String message) {
        super(message);
    }
}
