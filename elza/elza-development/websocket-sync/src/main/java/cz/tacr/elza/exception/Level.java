package cz.tacr.elza.exception;

/**
 * Úroveň vyjímky.
 *
 * @since 20.01.2017
 */
public enum Level {

    DANGER("danger"),

    WARNING("warning"),

    INFO("info");

    private String level;

    Level(final String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "Level{" +
                "level='" + level + '\'' +
                '}';
    }
}
