package ru.practicum.shareit.booking;

import ru.practicum.shareit.exception.UnsupportedStatusException;

import java.util.Arrays;
import java.util.Optional;

public enum BookingState {
    ALL,
    CURRENT,
    PAST,
    FUTURE,
    WAITING,
    REJECTED;

    public static BookingState from(String value) {
        return Optional.ofNullable(value)
                .flatMap(v -> Arrays.stream(values())
                        .filter(state -> state.name().equalsIgnoreCase(v))
                        .findFirst())
                .orElseThrow(() -> new UnsupportedStatusException("Unknown state: " + value));
    }
}
