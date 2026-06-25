package com.example.clickupsimplifier.clickup;

public record ConnectivityResult(Status status, ClickupUser user, String message) {

    public enum Status {
        OK,
        NOT_CONFIGURED,
        TOKEN_REJECTED,
        UNREACHABLE
    }

    public static ConnectivityResult ok(ClickupUser user) {
        return new ConnectivityResult(Status.OK, user, "Połączono z ClickUp");
    }

    public static ConnectivityResult notConfigured() {
        return new ConnectivityResult(Status.NOT_CONFIGURED, null, "Token nie jest skonfigurowany");
    }

    public static ConnectivityResult tokenRejected() {
        return new ConnectivityResult(Status.TOKEN_REJECTED, null, "Token odrzucony przez ClickUp (401)");
    }

    public static ConnectivityResult unreachable() {
        return new ConnectivityResult(Status.UNREACHABLE, null, "ClickUp nieosiągalny");
    }
}
