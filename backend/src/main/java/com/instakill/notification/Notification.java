package com.instakill.notification;

import java.util.List;

public record Notification(String title, String message, List<String> tags) {
}
