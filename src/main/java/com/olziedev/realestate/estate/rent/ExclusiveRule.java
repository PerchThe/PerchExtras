package com.olziedev.realestate.estate.rent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ExclusiveRule(String group) {

    public static boolean isRequested(String input, String tag) {
        if (input == null || tag == null || tag.isBlank()) return false;
        return Pattern.compile("(?:^|\\s)" + Pattern.quote(tag) + "(?:\\s|$)", Pattern.CASE_INSENSITIVE)
                .matcher(input)
                .find();
    }

    public static ExclusiveRule parse(String input, String tag) {
        if (input == null || tag == null || tag.isBlank()) return null;

        Matcher matcher = Pattern.compile(
                "(?:^|\\s)" + Pattern.quote(tag) + "\\s+([A-Za-z0-9_-]{1,32})(?=\\s|$)",
                Pattern.CASE_INSENSITIVE
        ).matcher(input);
        if (!matcher.find()) return null;

        return new ExclusiveRule(matcher.group(1).toLowerCase(Locale.ROOT));
    }
}
