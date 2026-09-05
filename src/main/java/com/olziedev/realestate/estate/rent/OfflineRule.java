package com.olziedev.realestate.estate.rent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record OfflineRule(int days) {

    public static final int MAX_DAYS = 36_500;

    public static boolean isRequested(String input, String tag) {
        if (input == null || tag == null || tag.isBlank()) return false;
        return Pattern.compile("(?:^|\\s)" + Pattern.quote(tag) + "(?:\\s|$)", Pattern.CASE_INSENSITIVE)
                .matcher(input)
                .find();
    }

    public static OfflineRule parse(String input, String tag) {
        if (input == null || tag == null || tag.isBlank()) return null;

        Matcher matcher = Pattern.compile(
                "(?:^|\\s)" + Pattern.quote(tag) + "\\s+(\\d{1,5})(?=\\s|$)",
                Pattern.CASE_INSENSITIVE
        ).matcher(input);
        if (!matcher.find()) return null;

        try {
            int days = Integer.parseInt(matcher.group(1));
            return days <= MAX_DAYS ? new OfflineRule(days) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
