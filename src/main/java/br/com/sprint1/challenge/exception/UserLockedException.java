package br.com.sprint1.challenge.exception;

import java.time.LocalDateTime;

public class UserLockedException extends RuntimeException {
    private final LocalDateTime lockedUntil;

    public UserLockedException(LocalDateTime lockedUntil) {
        super("Account locked until " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}