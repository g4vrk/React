package com.g4vrk.reactdataset.model;

public enum RecordLabel {
    SOFT("cheater", true),
    LEGIT("legit", false);

    private final String fileLabel;
    private final boolean cheater;

    private RecordLabel(String fileLabel, boolean cheater) {
        this.fileLabel = fileLabel;
        this.cheater = cheater;
    }

    public String fileLabel() {
        return this.fileLabel;
    }

    public boolean cheater() {
        return this.cheater;
    }

    public static RecordLabel parse(String input) {
        if (input == null) {
            return null;
        }
        if ("cheater".equalsIgnoreCase(input)) {
            return SOFT;
        }
        for (RecordLabel value : RecordLabel.values()) {
            if (!value.name().equalsIgnoreCase(input) && !value.fileLabel.equalsIgnoreCase(input)) continue;
            return value;
        }
        return null;
    }
}
