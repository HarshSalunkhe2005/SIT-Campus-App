package com.sit.campusbackend.complaint.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Works out a complaint's category: from what the student picked, or else from keywords in the description. */
public final class CategoryDetector {

    public static final String GENERAL = "General";

    /** What the report form sends, mapped to the canonical category (the same names the departments use as their type). */
    private static final Map<String, String> PICKED = Map.of(
            "ELECTRICAL", "Electrical",
            "PLUMBING", "Plumbing",
            "CIVIL", "Civil",
            "CLEANLINESS", "Cleaning",
            "CLEANING", "Cleaning",
            "IT", "IT",
            "FURNITURE", "Furniture",
            "HOSTEL", "Hostel",
            "OTHER", GENERAL,
            "GENERAL", GENERAL);

    /** Checked in order; the first category with a whole-word match wins. */
    private static final Map<String, Pattern> KEYWORDS = new LinkedHashMap<>();

    static {
        keywords("Electrical", "fan", "light", "electricity", "switch", "socket", "power", "wiring", "bulb", "voltage",
                "electric", "ac", "air conditioner");
        keywords("IT", "wifi", "internet", "network", "router", "laptop", "computer", "printer", "server", "cable", "portal");
        keywords("Cleaning", "clean", "garbage", "waste", "trash", "dirt", "sweep", "toilet", "bathroom", "dustbin", "smell",
                "hygiene", "floor", "cleaning", "mop");
        keywords("Plumbing", "pipe", "water", "tap", "leak", "drain", "plumber", "flush", "seepage", "tank", "sink", "basin");
        keywords("Hostel", "hostel", "room", "bed", "mattress", "mess", "canteen", "food", "warden", "dorm", "lift", "elevator");
        keywords("Furniture", "chair", "desk", "bench", "table", "door", "window", "lock", "handle", "cupboard", "almirah",
                "furniture", "wood");
        keywords("Civil", "wall", "paint", "crack", "ceiling", "tiles", "civil", "cement");
    }

    private CategoryDetector() {
    }

    /** Canonical category for a value from the form, or null when it is blank or not one the form offers. */
    public static String fromPicked(String picked) {
        if (picked == null || picked.isBlank()) return null;
        return PICKED.get(picked.trim().toUpperCase(Locale.ROOT));
    }

    public static String detect(String description) {
        if (description == null || description.isBlank()) return GENERAL;
        for (var entry : KEYWORDS.entrySet()) {
            if (entry.getValue().matcher(description).find()) return entry.getKey();
        }
        return GENERAL;
    }

    /** Whole words only (so "it" in "within" or "ac" in "place" never match), allowing a plural "s"/"es". */
    private static void keywords(String category, String... words) {
        String alternation = String.join("|", java.util.Arrays.stream(words).map(Pattern::quote).toList());
        KEYWORDS.put(category, Pattern.compile("\\b(?:" + alternation + ")(?:e?s)?\\b", Pattern.CASE_INSENSITIVE));
    }
}
