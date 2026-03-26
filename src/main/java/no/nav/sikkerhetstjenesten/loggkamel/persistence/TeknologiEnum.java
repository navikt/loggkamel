package no.nav.sikkerhetstjenesten.loggkamel.persistence;

public enum TeknologiEnum {
    POSTGRESQL("POSTGRESQL"),
    ORACLE("ORACLE"),
    DB2("DB2"),
    IMS("IMS");

    private final String displayName;

    TeknologiEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

