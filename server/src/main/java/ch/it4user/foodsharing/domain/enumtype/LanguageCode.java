package ch.it4user.foodsharing.domain.enumtype;

public enum LanguageCode {
    DE("de"),
    EN("en"),
    GWS("gws");

    private final String code;

    LanguageCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static LanguageCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DE;
        }
        for (LanguageCode value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        return DE;
    }
}
