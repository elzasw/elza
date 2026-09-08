package cz.tacr.elza.domain;

public enum BatchState {
    PREPARATION,
    IN_PROGRESS,
    TEST_IN_PROGRESS,
    TEST_FINISHED,
    PAUSED,
    FAILED,
    FINISHED,
    CANCELLED;

    public boolean isFinal() {
        return this == FINISHED || this == CANCELLED;
    }
}
