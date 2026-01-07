package com.olziedev.realestate.estate.rent;

import com.olziedev.realestate.utils.Configuration;
import com.olziedev.realestate.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public enum RentFlags {
    RENEW(Configuration.getString(Configuration.getConfig(), "settings.lines-rent-flags.renew"), Configuration.getString(Configuration.getConfig(), "settings.lines-rent-flags.renew-display")),
    NICEMODE(Configuration.getString(Configuration.getConfig(), "settings.lines-rent-flags.nicemode"), Configuration.getString(Configuration.getConfig(), "settings.lines-rent-flags.nicemode-display"));

    final String tag;
    final String display;

    RentFlags(String tag, String display) {
        this.tag = tag;
        this.display = Utils.color(display);
    }

    public String getTag() {
        return this.tag;
    }

    public String getDisplay() {
        return this.display;
    }

    public static List<RentFlags> getByTag(String tag) {
        List<RentFlags> rentFlags = new ArrayList<>();
        for (RentFlags flag : RentFlags.values()) {
            if (!tag.contains(flag.getTag())) continue;

            rentFlags.add(flag);
        }
        return rentFlags;
    }

    public static List<RentFlags> parse(String names) {
        if (names == null) return new ArrayList<>();

        List<RentFlags> rentFlags = new ArrayList<>();
        for (String name : names.split(",")) {
            try {
                rentFlags.add(RentFlags.valueOf(name.toUpperCase()));
            } catch (Throwable ignored) {}
        }
        return rentFlags;
    }
}
