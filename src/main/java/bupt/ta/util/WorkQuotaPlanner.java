package bupt.ta.util;

import bupt.ta.model.WorkArrangementItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a balanced workload recommendation from work-arrangement rows.
 */
public final class WorkQuotaPlanner {

    /** First integer or decimal in a duration string. */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private WorkQuotaPlanner() {
    }

    /**
     * Greedy LPT-style allocation: sort units by descending hours, assign each to the TA with least total hours so far.
     *
     * @param rows            structured work arrangement rows
     * @param plannedRecruits number of TAs to recruit (at least 1 used internally)
     * @return workload share estimates and summary statistics
     */
    public static Recommendation recommend(List<WorkArrangementItem> rows, int plannedRecruits) {
        // at least one recruit to avoid divide-by-zero
        int headcount = Math.max(1, plannedRecruits);
        // atomic work chunks to assign one-by-one
        List<WorkUnit> units = new ArrayList<>();
        // rows where duration text could not be parsed (still use default 1h)
        int unknownDurationRows = 0;
        if (rows != null) {
            for (WorkArrangementItem row : rows) {
                if (row == null) {
                    continue;
                }
                String workName = trim(row.getWorkName());
                int occurrences = Math.max(0, row.getResolvedOccurrenceCount());
                int rowTaCount = Math.max(0, row.getTaCount());
                if (workName.isEmpty() || occurrences < 1 || rowTaCount < 1) {
                    continue;
                }
                DurationParse parsed = parseDurationHours(row.getResolvedSessionDuration());
                if (!parsed.known) {
                    unknownDurationRows++;
                }
                int totalUnits = occurrences * rowTaCount;
                for (int i = 0; i < totalUnits; i++) {
                    units.add(new WorkUnit(workName, parsed.hours));
                }
            }
        }

        List<TAQuota> quotas = new ArrayList<>();
        for (int i = 0; i < headcount; i++) {
            quotas.add(new TAQuota("Share " + (i + 1)));
        }
        units.sort(Comparator.comparingDouble((WorkUnit u) -> u.hours).reversed());
        for (WorkUnit unit : units) {
            quotas.sort(Comparator.comparingDouble(TAQuota::getTotalHours));
            TAQuota picked = quotas.get(0);
            picked.totalHours += unit.hours;
            picked.workCounts.put(unit.workName, picked.workCounts.getOrDefault(unit.workName, 0) + 1);
        }
        // Keep a stable label order after repeatedly sorting by current load.
        quotas.sort(Comparator.comparing(TAQuota::getName));

        double totalHours = units.stream().mapToDouble(u -> u.hours).sum();
        double averageHours = headcount > 0 ? totalHours / headcount : 0.0;
        double max = quotas.stream().mapToDouble(TAQuota::getTotalHours).max().orElse(0.0);
        double min = quotas.stream().mapToDouble(TAQuota::getTotalHours).min().orElse(0.0);
        return new Recommendation(quotas, totalHours, averageHours, max - min, unknownDurationRows);
    }

    /**
     * Parse human-readable duration into hours; on failure use 1h and {@code known == false}.
     */
    private static DurationParse parseDurationHours(String raw) {
        String text = trim(raw).toLowerCase(Locale.ROOT);
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (!matcher.find()) {
            return new DurationParse(1.0, false);
        }
        double value;
        try {
            value = Double.parseDouble(matcher.group(1));
        } catch (Exception ex) {
            return new DurationParse(1.0, false);
        }
        if (value <= 0) {
            return new DurationParse(1.0, false);
        }
        if (text.contains("min") || text.contains("minute")) {
            return new DurationParse(value / 60.0, true);
        }
        return new DurationParse(value, true);
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    /** Summary of one recommendation run (API surface for JSP / callers). */
    public static final class Recommendation {
        private final List<TAQuota> quotas;
        private final double totalHours;
        private final double averageHours;
        private final double imbalanceHours;
        private final int unknownDurationRows;

        Recommendation(List<TAQuota> quotas, double totalHours, double averageHours, double imbalanceHours, int unknownDurationRows) {
            this.quotas = quotas;
            this.totalHours = totalHours;
            this.averageHours = averageHours;
            this.imbalanceHours = imbalanceHours;
            this.unknownDurationRows = unknownDurationRows;
        }

        public List<TAQuota> getQuotas() {
            return quotas;
        }

        public double getTotalHours() {
            return totalHours;
        }

        public double getAverageHours() {
            return averageHours;
        }

        public double getImbalanceHours() {
            return imbalanceHours;
        }

        public int getUnknownDurationRows() {
            return unknownDurationRows;
        }
    }

    /** One virtual workload share: running total hours and per-work-name counts. */
    public static final class TAQuota {
        private final String name;
        private double totalHours;
        private final Map<String, Integer> workCounts = new LinkedHashMap<>();

        TAQuota(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public double getTotalHours() {
            return totalHours;
        }

        public Map<String, Integer> getWorkCounts() {
            return workCounts;
        }
    }

    private static final class WorkUnit {
        private final String workName;
        private final double hours;

        private WorkUnit(String workName, double hours) {
            this.workName = workName;
            this.hours = hours;
        }
    }

    private static final class DurationParse {
        private final double hours;
        private final boolean known;

        private DurationParse(double hours, boolean known) {
            this.hours = hours;
            this.known = known;
        }
    }
}
