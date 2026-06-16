package com.athena.badge.service;

import com.athena.common.event.BadgeSuggestion;

import java.util.regex.Pattern;

public final class BadgeSuggestionValidator {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{2,39}$");
    private static final int NAME_MAX = 100;
    private static final int DESC_MAX = 500;
    private static final int ICON_MAX = 40;

    public static boolean isValid(BadgeSuggestion suggestion) {
        if (suggestion == null || suggestion.code() == null || !CODE.matcher(suggestion.code()).matches()) {
            return false;
        }
        if (suggestion.name() == null || suggestion.name().isBlank() || suggestion.name().length() > NAME_MAX) {
            return false;
        }
        if (suggestion.description() != null && suggestion.description().length() > DESC_MAX) {
            return false;
        }
        return suggestion.icon() == null || suggestion.icon().length() <= ICON_MAX;
    }
}
