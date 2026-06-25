package com.example.clickupsimplifier.clickup;

/**
 * Tozsamosc zalogowanego uzytkownika ClickUp (z {@code GET /user}).
 *
 * @param id       identyfikator uzytkownika ClickUp
 * @param username nazwa uzytkownika
 */
public record ClickupUser(String id, String username) {
}
