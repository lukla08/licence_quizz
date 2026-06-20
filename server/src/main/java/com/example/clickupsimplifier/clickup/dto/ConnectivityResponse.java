package com.example.clickupsimplifier.clickup.dto;

import com.example.clickupsimplifier.clickup.ClickupUser;

public record ConnectivityResponse(String status, ClickupUser user, String message) {
}
