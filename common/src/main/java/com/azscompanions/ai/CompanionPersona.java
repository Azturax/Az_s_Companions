package com.azscompanions.ai;

import com.azscompanions.cci.CciCompanionParams;

import java.util.Locale;
import java.util.Objects;

/**
 * Per-companion AI identity customization (who / what / how + optional flavor).
 * Empty fields fall back to the generic system prompt — each companion mind stays independent.
 */
public final class CompanionPersona {
    /** Per-field persona text limit (multi-sentence answers allowed). */
    public static final int MAX_LEN = 2048;
    public static final CompanionPersona EMPTY = new CompanionPersona("", "", "", "", "", "", false);

    public static final String NBT_WHO = "WhoAmI";
    public static final String NBT_WHAT = "WhatAmIDoing";
    public static final String NBT_HOW = "HowWillIBe";
    public static final String NBT_SPEECH = "SpeechStyle";
    public static final String NBT_RELATIONSHIP = "RelationshipToOwner";
    public static final String NBT_QUIRKS = "Quirks";
    public static final String NBT_INITIALIZED = "PersonaInitialized";

    private final String whoAmI;
    private final String whatAmIDoing;
    private final String howWillIBe;
    private final String speechStyle;
    private final String relationshipToOwner;
    private final String quirks;
    private final boolean initialized;

    public CompanionPersona(
            String whoAmI,
            String whatAmIDoing,
            String howWillIBe,
            String speechStyle,
            String relationshipToOwner,
            String quirks,
            boolean initialized
    ) {
        this.whoAmI = sanitize(whoAmI);
        this.whatAmIDoing = sanitize(whatAmIDoing);
        this.howWillIBe = sanitize(howWillIBe);
        this.speechStyle = sanitize(speechStyle);
        this.relationshipToOwner = sanitize(relationshipToOwner);
        this.quirks = sanitize(quirks);
        this.initialized = initialized;
    }

    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim().replace('\0', ' ');
        if (t.length() > MAX_LEN) {
            t = t.substring(0, MAX_LEN);
        }
        return t;
    }

    public String whoAmI() {
        return whoAmI;
    }

    public String whatAmIDoing() {
        return whatAmIDoing;
    }

    public String howWillIBe() {
        return howWillIBe;
    }

    public String speechStyle() {
        return speechStyle;
    }

    public String relationshipToOwner() {
        return relationshipToOwner;
    }

    public String quirks() {
        return quirks;
    }

    public boolean initialized() {
        return initialized;
    }

    public boolean isBlank() {
        return whoAmI.isEmpty() && whatAmIDoing.isEmpty() && howWillIBe.isEmpty()
                && speechStyle.isEmpty() && relationshipToOwner.isEmpty() && quirks.isEmpty();
    }

    public CompanionPersona withInitialized(boolean value) {
        return new CompanionPersona(whoAmI, whatAmIDoing, howWillIBe, speechStyle, relationshipToOwner, quirks, value);
    }

    public CompanionPersona cleared() {
        return new CompanionPersona("", "", "", "", "", "", true);
    }

    public CompanionPersona withField(String key, String value) {
        String k = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        String v = sanitize(value);
        return switch (k) {
            case "who", "whoami", "identity", "backstory" ->
                    new CompanionPersona(v, whatAmIDoing, howWillIBe, speechStyle, relationshipToOwner, quirks, true);
            case "what", "whatamidoing", "purpose", "goal", "job" ->
                    new CompanionPersona(whoAmI, v, howWillIBe, speechStyle, relationshipToOwner, quirks, true);
            case "how", "howwillibe", "personality", "tone", "manner" ->
                    new CompanionPersona(whoAmI, whatAmIDoing, v, speechStyle, relationshipToOwner, quirks, true);
            case "speech", "speechstyle", "style" ->
                    new CompanionPersona(whoAmI, whatAmIDoing, howWillIBe, v, relationshipToOwner, quirks, true);
            case "relationship", "relationshiptoowner", "owner" ->
                    new CompanionPersona(whoAmI, whatAmIDoing, howWillIBe, speechStyle, v, quirks, true);
            case "quirks", "quirk" ->
                    new CompanionPersona(whoAmI, whatAmIDoing, howWillIBe, speechStyle, relationshipToOwner, v, true);
            default -> this;
        };
    }

    /**
     * Merge CCI {@code who=}/{@code what=}/{@code how=} (and optional extras) onto this persona.
     * Returns this instance unchanged when no persona keys are present.
     */
    public CompanionPersona mergeFromCci(CciCompanionParams params) {
        if (params == null) {
            return this;
        }
        boolean any = false;
        String who = whoAmI;
        String what = whatAmIDoing;
        String how = howWillIBe;
        String speech = speechStyle;
        String rel = relationshipToOwner;
        String q = quirks;
        String v;
        // CCI keys are lowercased by CciCompanionParams; accept whoAmI / whatAmIDoing / howWillIBe.
        v = params.first("whoami", "who", "identity", "backstory");
        if (v != null) {
            who = sanitize(v);
            any = true;
        }
        v = params.first("whatamidoing", "what", "purpose", "goal", "job");
        if (v != null) {
            what = sanitize(v);
            any = true;
        }
        v = params.first("howwillibe", "how", "personality", "tone", "manner");
        if (v != null) {
            how = sanitize(v);
            any = true;
        }
        v = params.first("speech", "speechstyle", "style");
        if (v != null) {
            speech = sanitize(v);
            any = true;
        }
        v = params.first("relationship", "relationshiptoowner", "ownerbond");
        if (v != null) {
            rel = sanitize(v);
            any = true;
        }
        v = params.first("quirks", "quirk");
        if (v != null) {
            q = sanitize(v);
            any = true;
        }
        if (!any) {
            return this;
        }
        return new CompanionPersona(who, what, how, speech, rel, q, true);
    }

    public static boolean hasPersonaKeys(CciCompanionParams params) {
        if (params == null) {
            return false;
        }
        return params.first("whoami", "who", "identity", "backstory") != null
                || params.first("whatamidoing", "what", "purpose", "goal", "job") != null
                || params.first("howwillibe", "how", "personality", "tone", "manner") != null
                || params.first("speech", "speechstyle", "style") != null
                || params.first("relationship", "relationshiptoowner", "ownerbond") != null
                || params.first("quirks", "quirk") != null;
    }

    /** True when a brand-new primary companion should get first-create onboarding (not recall). */
    public static boolean shouldOfferOnboarding(boolean fightSpawn, boolean child, boolean initialized) {
        return !fightSpawn && !child && !initialized;
    }

    /** Appendix injected into the LLM system prompt for this companion mind. */
    public String promptAppendix() {
        if (isBlank()) {
            return " Persona: use a friendly, helpful companion voice with no special backstory unless the player defines one.";
        }
        StringBuilder sb = new StringBuilder(" Persona (stay in character; this is your mind only):");
        if (!whoAmI.isEmpty()) {
            sb.append(" Who you are: ").append(whoAmI).append('.');
        }
        if (!whatAmIDoing.isEmpty()) {
            sb.append(" What you are doing: ").append(whatAmIDoing).append('.');
        }
        if (!howWillIBe.isEmpty()) {
            sb.append(" How you will be: ").append(howWillIBe).append('.');
        }
        if (!speechStyle.isEmpty()) {
            sb.append(" Speech style: ").append(speechStyle).append('.');
        }
        if (!relationshipToOwner.isEmpty()) {
            sb.append(" Relationship to owner: ").append(relationshipToOwner).append('.');
        }
        if (!quirks.isEmpty()) {
            sb.append(" Quirks: ").append(quirks).append('.');
        }
        return sb.toString();
    }

    /** Multi-line summary for `/az persona` chat output. */
    public String formatSummary(String companionName) {
        String name = companionName == null || companionName.isBlank() ? "Companion" : companionName.trim();
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" persona");
        sb.append(initialized ? " (set)" : " (not set yet)");
        sb.append('\n');
        sb.append("  who: ").append(blankDash(whoAmI)).append('\n');
        sb.append("  what: ").append(blankDash(whatAmIDoing)).append('\n');
        sb.append("  how: ").append(blankDash(howWillIBe)).append('\n');
        sb.append("  speech: ").append(blankDash(speechStyle)).append('\n');
        sb.append("  relationship: ").append(blankDash(relationshipToOwner)).append('\n');
        sb.append("  quirks: ").append(blankDash(quirks));
        return sb.toString();
    }

    private static String blankDash(String v) {
        return v == null || v.isBlank() ? "(empty — generic prompt)" : v;
    }

    public static String onboardingIntro(String companionName) {
        String name = companionName == null || companionName.isBlank() ? "I" : companionName.trim();
        return "Hi! Before we adventure, help define me. Who am I? What am I doing? How will I be?"
                + " Optional: speech style, relationship, quirks."
                + " Open the Persona setup, or use /az persona set who|what|how|speech|relationship|quirks <text>.";
    }

    public static String onboardingLineWho() {
        return "Who am I? Tell me my identity, role, or backstory.";
    }

    public static String onboardingLineWhat() {
        return "What am I doing? What is my purpose, job, or goal with you?";
    }

    public static String onboardingLineHow() {
        return "How will I be? Personality, tone, mannerisms, values.";
    }

    public static String onboardingLineSpeech() {
        return "Speech style? (optional) How I talk — slang, formality, catchphrases.";
    }

    public static String onboardingLineRelationship() {
        return "Relationship to you? (optional) Friend, partner, rival, guardian…";
    }

    public static String onboardingLineQuirks() {
        return "Quirks? (optional) Habits, likes, oddities.";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompanionPersona that)) {
            return false;
        }
        return initialized == that.initialized
                && Objects.equals(whoAmI, that.whoAmI)
                && Objects.equals(whatAmIDoing, that.whatAmIDoing)
                && Objects.equals(howWillIBe, that.howWillIBe)
                && Objects.equals(speechStyle, that.speechStyle)
                && Objects.equals(relationshipToOwner, that.relationshipToOwner)
                && Objects.equals(quirks, that.quirks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(whoAmI, whatAmIDoing, howWillIBe, speechStyle, relationshipToOwner, quirks, initialized);
    }
}
