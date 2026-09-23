package cz.tacr.elza.service.dms;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of one {@link DmsConsistencyService#check(boolean, boolean)} run.
 * Each entry's {@code count} is the true total; {@code sample} holds at most
 * the first 500 items in that category.
 */
public final class DmsConsistencyReport {

    private static final int MAX_SAMPLE_SIZE = 500;

    public final Entry missing = new Entry();
    public final Entry orphans = new Entry();
    public final Entry sizeMismatch = new Entry();
    public final Entry corrupted = new Entry();
    public final Entry notMigrated = new Entry();
    public final Entry staleTmp = new Entry();
    public final Entry foreign = new Entry();
    public String trashDirUsed;
    public long durationMillis;

    public static final class Entry {
        public int count;
        public final List<String> sample = new ArrayList<>();

        void add(String item) {
            count++;
            if (sample.size() < MAX_SAMPLE_SIZE) {
                sample.add(item);
            }
        }
    }
}
