package com.olziedev.realestate.estate.rent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record NoExtendRule(String group, int cooldownDays) {

    public static final int MAX_COOLDOWN_DAYS = 36_500;

    public static boolean isRequested(String input, String tag) {
        if (input == null || tag == null || tag.isBlank()) return false;
        return Pattern.compile("(?:^|\\s)" + Pattern.quote(tag) + "(?:\\s|$)", Pattern.CASE_INSENSITIVE)
                .matcher(input)
                .find();
    }

    public static NoExtendRule parse(String input, String tag) {
        if (input == null || tag == null || tag.isBlank()) return null;

        Pattern pattern = Pattern.compile(
                "(?:^|\\s)" + Pattern.quote(tag)
                        + "\\s+([A-Za-z0-9_-]{1,32})\\s+(\\d{1,5})(?=\\s|$)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) return null;

        int days;
        try {
            days = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (days > MAX_COOLDOWN_DAYS) return null;

        return new NoExtendRule(matcher.group(1).toLowerCase(Locale.ROOT), days);
    }

    public long cooldownMillis() {
        return cooldownDays * 86_400_000L;
    }
}
