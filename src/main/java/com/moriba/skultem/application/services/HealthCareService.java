package com.moriba.skultem.application.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.HealthCareReportDTOs.Bucket;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Check;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.MedicineRow;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Periods;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.TermOption;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.TrendRow;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Trends;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.YearOption;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.ClassRow;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.DayRow;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Continuable;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Count;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.FrequentVisitor;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Limit;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Limits;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.MedicationCheck;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Overview;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.StudentHistory;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Summary;
import com.moriba.skultem.application.dto.HealthCareReportDTOs.Today;
import com.moriba.skultem.application.dto.HealthCareSettingDTO;
import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.Complaint;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.repository.HealthCareSettingRepository;
import com.moriba.skultem.domain.repository.HealthCareTurnedAwayRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository.Filter;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.HealthCareLimits;
import com.moriba.skultem.domain.shared.SchoolTimeZone;

import lombok.RequiredArgsConstructor;

/** Reads for Health Care (docs/HEALTH_CARE.md §8, §10): the dashboard, lists, checks before acting, and reports. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HealthCareService {

    /** Longest range a report covers, as for library reports. */
    public static final int MAX_RANGE_DAYS = 92;
    /** "Frequent visitors": this many counted visits or more in the range (§10). */
    public static final int FREQUENT_VISITS = 3;

    private final HealthCareVisitRepository visitRepo;
    private final HealthCareTurnedAwayRepository turnedAwayRepo;
    private final HealthCareSettingRepository settingRepo;
    private final StudentRepository studentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TermRepository termRepo;
    private final HealthCareSupport support;

    public HealthCareSettingDTO settings(String schoolId) {
        var saved = settingRepo.findBySchool(schoolId);
        return HealthCareDtoMapper.toDTO(saved.orElseGet(() -> HealthCareSetting.defaults(schoolId)), saved.isPresent());
    }

    public Overview overview(String schoolId) {
        var setting = support.settings(schoolId);
        Instant now = support.now();
        var open = visitRepo.findOpen(schoolId).stream().map(v -> HealthCareDtoMapper.toDTO(v, setting, now)).toList();
        var today = support.today();
        Instant from = startOf(today);
        Instant to = startOf(today.plusDays(1));
        var visits = visitRepo.findBetween(schoolId, from, to).stream().filter(v -> !v.isVoided()).toList();
        long turnedAway = turnedAwayRepo.findBetween(schoolId, from, to).size();
        var counts = new Today(visits.size(), visits.stream().filter(HealthCareVisit::isOpen).count(),
                outcome(visits, Outcome.RETURNED_TO_CLASS), outcome(visits, Outcome.SENT_HOME),
                outcome(visits, Outcome.REFERRED), turnedAway);
        return new Overview(open.stream().filter(v -> !v.needsFollowUp()).toList(),
                open.stream().filter(HealthCareVisitDTO::needsFollowUp).toList(), counts, setting.getLongStayMinutes());
    }

    private static long outcome(List<HealthCareVisit> visits, Outcome outcome) {
        return visits.stream().filter(v -> v.isClosed() && v.getOutcome() == outcome).count();
    }

    public Page<HealthCareVisitDTO> listVisits(String schoolId, int page, int size, LocalDate from, LocalDate to,
            Filter filter, Outcome outcome, String complaint, boolean overLimitOnly, String studentId, String query) {
        var setting = support.settings(schoolId);
        Instant now = support.now();
        var search = new HealthCareVisitRepository.Search(from == null ? null : startOf(from),
                to == null ? null : startOf(to.plusDays(1)), filter, outcome, complaint, overLimitOnly, studentId,
                query, now);
        return visitRepo.search(schoolId, search, PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, size), 100)))
                .map(v -> HealthCareDtoMapper.toDTO(v, setting, now));
    }

    public HealthCareVisitDTO visit(String schoolId, String id) {
        var visit = visitRepo.findByIdAndSchool(id, schoolId).orElseThrow(() -> new NotFoundException("Visit not found"));
        return HealthCareDtoMapper.toDTO(visit, support.settings(schoolId), support.now());
    }

    /**
     * What the nurse sees after picking a student (and complaints), before starting a visit: recent visits (V-3),
     * whether one is already open (V-1), and what the limit rules say (§6.2). Records nothing.
     */
    public Check check(String schoolId, String studentId, List<String> complaintCodes) {
        var setting = support.settings(schoolId);
        var student = support.currentStudent(schoolId, studentId);
        var today = support.today();
        var term = support.termOn(schoolId, today);
        Instant now = support.now();

        // Unknown or hidden codes simply don't count as urgent here; starting the visit reports them properly.
        boolean urgent = complaintCodes != null && complaintCodes.stream()
                .map(setting::complaint).flatMap(java.util.Optional::stream).anyMatch(c -> c.urgent() && !c.hidden());

        // From the start of the term or month (whichever is earlier): covers every period a rule can use.
        var recent = visitRepo.findByStudentSince(schoolId, studentId, support.countingSince(today, term));
        List<Instant> counted = recent.stream().filter(HealthCareVisit::countsTowardsLimits)
                .map(HealthCareVisit::getArrivedAt).toList();
        var week = HealthCareLimits.window(HealthCareSetting.Period.WEEK, today, term);
        var termWindow = HealthCareLimits.window(HealthCareSetting.Period.TERM, today, term);

        var last = visitRepo.findByStudent(schoolId, studentId).stream().filter(v -> !v.isVoided()).findFirst();
        var open = visitRepo.findOpenByStudent(schoolId, studentId);
        var limit = support.checkLimits(setting, student, urgent, counted, today, term);

        var continuable = recent.stream()
                .filter(v -> v.isClosed() && v.arrivalDate().equals(today))
                .sorted(Comparator.comparing(HealthCareVisit::getArrivedAt).reversed())
                .map(v -> new Continuable(v.getId(), v.getArrivedAt(), labels(v.getComplaints()),
                        v.getOutcome() == null ? null : v.getOutcome().name()))
                .toList();

        return new Check(studentId, student.student().getName(), student.className(),
                HealthCareLimits.countBetween(counted, week[0], week[1]),
                termWindow == null ? 0 : HealthCareLimits.countBetween(counted, termWindow[0], termWindow[1]),
                last.map(HealthCareVisit::getArrivedAt).orElse(null),
                last.map(v -> labels(v.getComplaints())).orElse(null),
                open.map(HealthCareVisit::getId).orElse(null), toLimit(limit), continuable);
    }

    private static Limit toLimit(HealthCareLimits.Check c) {
        if (!c.reached()) {
            return new Limit("ALLOW", null, null, null, 0, c.urgent(), false, false, null);
        }
        return new Limit(c.result().name(), c.rule().name(), c.rule().maxVisits(), c.rule().period().name(),
                c.count(), c.urgent(), c.refused(), c.needsReason(), c.message());
    }

    /** Whether this medicine was already given to the visit's student within the school's window (M-2). */
    public MedicationCheck medicationCheck(String schoolId, String visitId, String name) {
        var setting = support.settings(schoolId);
        if (setting.getSameMedicineHours() == null || name == null || name.isBlank()) {
            return null;
        }
        var visit = visitRepo.findByIdAndSchool(visitId, schoolId).orElseThrow(() -> new NotFoundException("Visit not found"));
        Instant now = support.now();
        return support.lastSameMedicine(schoolId, visit.getStudentId(), name, setting.getSameMedicineHours(), now)
                .map(m -> {
                    long minutes = Math.max(0, Duration.between(m.givenAt(), now).toMinutes());
                    String action = setting.getSameMedicineAction().name();
                    String message = m.name() + " was already given " + ago(minutes) + " (" + m.dose() + ")."
                            + ("REASON".equals(action) ? " Give a reason to record it again." : "");
                    return new MedicationCheck(m.name(), m.givenAt(), minutes, setting.getSameMedicineHours(), action,
                            message);
                }).orElse(null);
    }

    private static String ago(long minutes) {
        return minutes < 60 ? minutes + " min ago" : (minutes / 60) + " h " + (minutes % 60) + " min ago";
    }

    // Reports (§10). Voided visits are never counted; open ones count as "still open".

    public List<HealthCareVisitDTO> dailyLog(String schoolId, LocalDate date) {
        var setting = support.settings(schoolId);
        Instant now = support.now();
        return visitRepo.findBetween(schoolId, startOf(date), startOf(date.plusDays(1))).stream()
                .filter(v -> !v.isVoided())
                .sorted(Comparator.comparing(HealthCareVisit::getArrivedAt))
                .map(v -> HealthCareDtoMapper.toDTO(v, setting, now)).toList();
    }

    public Summary summary(String schoolId, LocalDate from, LocalDate to) {
        requireRange(from, to);
        var visits = counted(schoolId, from, to);
        long turnedAway = turnedAwayRepo.findBetween(schoolId, startOf(from), startOf(to.plusDays(1))).size();

        Map<String, Count> byComplaint = new LinkedHashMap<>();
        for (var v : visits) {
            for (var c : v.getComplaints()) {
                byComplaint.merge(c.code(), new Count(c.code(), c.label(), 1),
                        (a, b) -> new Count(a.key(), a.label(), a.count() + 1));
            }
        }
        List<Count> byOutcome = new ArrayList<>();
        for (var o : Outcome.values()) {
            byOutcome.add(new Count(o.name(), outcomeLabel(o), outcome(visits, o)));
        }
        byOutcome.add(new Count("OPEN", "Still open", visits.stream().filter(HealthCareVisit::isOpen).count()));

        Map<String, List<HealthCareVisit>> byClass = new LinkedHashMap<>();
        for (var v : visits) {
            String cls = v.getStudentClass() == null || v.getStudentClass().isBlank() ? "No class" : v.getStudentClass();
            byClass.computeIfAbsent(cls, k -> new ArrayList<>()).add(v);
        }
        List<ClassRow> classes = byClass.entrySet().stream()
                .map(e -> new ClassRow(e.getKey(), e.getValue().size(),
                        e.getValue().stream().map(HealthCareVisit::getStudentId).distinct().count(),
                        outcome(e.getValue(), Outcome.RETURNED_TO_CLASS), outcome(e.getValue(), Outcome.SENT_HOME),
                        outcome(e.getValue(), Outcome.REFERRED), e.getValue().stream().filter(HealthCareVisit::isOpen).count()))
                .sorted(Comparator.comparingLong(ClassRow::visits).reversed().thenComparing(ClassRow::className))
                .toList();

        List<DayRow> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate day = d;
            var onDay = visits.stream().filter(v -> v.arrivalDate().equals(day)).toList();
            days.add(new DayRow(day, onDay.size(), outcome(onDay, Outcome.SENT_HOME), outcome(onDay, Outcome.REFERRED)));
        }

        // Average time in the sick bay, over visits closed with a real departure time.
        var minutes = visits.stream().filter(v -> v.isClosed() && v.getLeftAt() != null)
                .map(v -> v.minutesIn(v.getLeftAt())).filter(java.util.Objects::nonNull).mapToLong(Long::longValue);
        var average = minutes.average();

        return new Summary(from, to, visits.size(), visits.stream().map(HealthCareVisit::getStudentId).distinct().count(),
                visits.stream().filter(HealthCareVisit::isUrgent).count(),
                visits.stream().filter(HealthCareVisit::isOpen).count(),
                visits.stream().filter(v -> v.getOverLimit() != null).count(), turnedAway,
                average.isPresent() ? Math.round(average.getAsDouble()) : null,
                sorted(byComplaint.values()), byOutcome, classes, days);
    }

    public List<FrequentVisitor> frequentVisitors(String schoolId, LocalDate from, LocalDate to) {
        requireRange(from, to);
        Map<String, List<HealthCareVisit>> byStudent = new LinkedHashMap<>();
        for (var v : counted(schoolId, from, to)) {
            byStudent.computeIfAbsent(v.getStudentId(), k -> new ArrayList<>()).add(v);
        }
        return byStudent.values().stream()
                .filter(list -> list.size() >= FREQUENT_VISITS)
                .map(list -> {
                    list.sort(Comparator.comparing(HealthCareVisit::getArrivedAt));
                    var first = list.get(list.size() - 1);
                    var complaints = new LinkedHashSet<String>();
                    list.forEach(v -> v.getComplaints().forEach(c -> complaints.add(c.label())));
                    return new FrequentVisitor(first.getStudentId(), first.getStudentName(), first.getStudentClass(),
                            list.size(), list.stream().map(HealthCareVisit::getArrivedAt).toList(),
                            List.copyOf(complaints));
                })
                .sorted(Comparator.comparingLong(FrequentVisitor::visits).reversed()
                        .thenComparing(FrequentVisitor::studentName))
                .toList();
    }

    public Limits limits(String schoolId, LocalDate from, LocalDate to) {
        requireRange(from, to);
        var setting = support.settings(schoolId);
        Instant now = support.now();
        var overLimit = counted(schoolId, from, to).stream().filter(v -> v.getOverLimit() != null)
                .map(v -> HealthCareDtoMapper.toDTO(v, setting, now)).toList();
        var turnedAway = turnedAwayRepo.findBetween(schoolId, startOf(from), startOf(to.plusDays(1))).stream()
                .map(HealthCareDtoMapper::toDTO).toList();
        return new Limits(from, to, overLimit, turnedAway);
    }

    public StudentHistory studentHistory(String schoolId, String studentId) {
        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        var setting = support.settings(schoolId);
        Instant now = support.now();
        return new StudentHistory(studentId, student.getName(),
                visitRepo.findByStudent(schoolId, studentId).stream().map(v -> HealthCareDtoMapper.toDTO(v, setting, now))
                        .toList(),
                turnedAwayRepo.findByStudent(schoolId, studentId).stream().map(HealthCareDtoMapper::toDTO).toList());
    }

    // Trends (complaints by week, month or term)

    public enum TrendScope {
        MONTH, TERM, YEAR
    }

    private static final java.time.format.DateTimeFormatter DAY_MONTH = java.time.format.DateTimeFormatter.ofPattern("d MMM");
    private static final java.time.format.DateTimeFormatter MONTH_YEAR = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");

    /** The school's academic years, newest first, each with its terms - what a trends report can cover. */
    public Periods periods(String schoolId) {
        var years = academicYearRepo.findAllBySchool(schoolId, PageRequest.of(0, 50)).getContent().stream()
                .sorted(Comparator.comparing(y -> y.getStartDate() == null ? LocalDate.MIN : y.getStartDate(),
                        Comparator.reverseOrder()))
                .map(y -> new YearOption(y.getId(), y.getName(), y.getStartDate(), y.getEndDate(),
                        termsOf(schoolId, y.getId()).stream()
                                .map(t -> new TermOption(t.getId(), t.getName(), t.getStartDate(), t.getEndDate()))
                                .toList()))
                .toList();
        var activeYear = academicYearRepo.findActiveBySchool(schoolId).orElse(null);
        String activeTerm = null;
        if (activeYear != null) {
            var today = support.today();
            activeTerm = termsOf(schoolId, activeYear.getId()).stream()
                    .filter(t -> !today.isBefore(t.getStartDate()) && !today.isAfter(t.getEndDate()))
                    .map(com.moriba.skultem.domain.model.Term::getId).findFirst()
                    .orElse(termRepo.findActiveBySchoolAndAcademicYear(schoolId, activeYear.getId())
                            .map(com.moriba.skultem.domain.model.Term::getId).orElse(null));
        }
        return new Periods(activeYear == null ? null : activeYear.getId(), activeTerm, years);
    }

    private List<com.moriba.skultem.domain.model.Term> termsOf(String schoolId, String yearId) {
        return termRepo.findByAcademicYearIdAndSchool(yearId, schoolId).stream()
                .filter(t -> t.getStartDate() != null && t.getEndDate() != null)
                .sorted(Comparator.comparing(com.moriba.skultem.domain.model.Term::getStartDate))
                .toList();
    }

    /**
     * Complaints and other figures over a month (columns: weeks), a term (columns: months) or an academic year
     * (columns: terms). {@code month} is "yyyy-MM" for MONTH; {@code termId} for TERM; {@code yearId} for YEAR.
     */
    public Trends trends(String schoolId, TrendScope scope, String month, String termId, String yearId) {
        List<Bucket> buckets = new ArrayList<>();
        LocalDate from;
        LocalDate to;
        String title;
        switch (scope) {
            case MONTH -> {
                java.time.YearMonth ym;
                try {
                    ym = java.time.YearMonth.parse(month == null ? "" : month.trim());
                } catch (java.time.format.DateTimeParseException e) {
                    throw new BadRequestException("Choose a month");
                }
                from = ym.atDay(1);
                to = ym.atEndOfMonth();
                title = ym.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"));
                int week = 1;
                for (LocalDate start = from; !start.isAfter(to); start = start.plusDays(7), week++) {
                    LocalDate end = start.plusDays(6).isAfter(to) ? to : start.plusDays(6);
                    buckets.add(new Bucket("W" + week, start.getDayOfMonth() + "–" + end.format(DAY_MONTH), start, end));
                }
            }
            case TERM -> {
                var term = termRepo.findByIdAndSchoolId(termId == null ? "" : termId, schoolId)
                        .filter(t -> t.getStartDate() != null && t.getEndDate() != null)
                        .orElseThrow(() -> new NotFoundException("Term not found"));
                from = term.getStartDate();
                to = term.getEndDate();
                title = term.getName();
                for (var ym = java.time.YearMonth.from(from); !ym.atDay(1).isAfter(to); ym = ym.plusMonths(1)) {
                    LocalDate start = ym.atDay(1).isBefore(from) ? from : ym.atDay(1);
                    LocalDate end = ym.atEndOfMonth().isAfter(to) ? to : ym.atEndOfMonth();
                    buckets.add(new Bucket(ym.toString(), ym.format(MONTH_YEAR), start, end));
                }
            }
            default -> {
                var year = academicYearRepo.findByIdAndSchoolId(yearId == null ? "" : yearId, schoolId)
                        .orElseThrow(() -> new NotFoundException("Academic year not found"));
                var terms = termsOf(schoolId, year.getId());
                if (terms.isEmpty() && (year.getStartDate() == null || year.getEndDate() == null)) {
                    throw new BadRequestException("This academic year has no dates or terms yet");
                }
                from = year.getStartDate() != null ? year.getStartDate() : terms.get(0).getStartDate();
                to = year.getEndDate() != null ? year.getEndDate() : terms.get(terms.size() - 1).getEndDate();
                title = year.getName();
                if (terms.isEmpty()) {
                    for (var ym = java.time.YearMonth.from(from); !ym.atDay(1).isAfter(to); ym = ym.plusMonths(1)) {
                        LocalDate start = ym.atDay(1).isBefore(from) ? from : ym.atDay(1);
                        LocalDate end = ym.atEndOfMonth().isAfter(to) ? to : ym.atEndOfMonth();
                        buckets.add(new Bucket(ym.toString(), ym.format(MONTH_YEAR), start, end));
                    }
                } else {
                    for (var t : terms) {
                        buckets.add(new Bucket(t.getId(), t.getName(), t.getStartDate(), t.getEndDate()));
                    }
                }
            }
        }

        var visits = counted(schoolId, from, to);
        var turnedAway = turnedAwayRepo.findBetween(schoolId, startOf(from), startOf(to.plusDays(1)));

        // Each visit to the first bucket holding its date; anything else (holidays between terms) to "Outside terms".
        // Only the dated buckets: "Outside terms", added below, has no dates and takes whatever none of them hold.
        final int dated = buckets.size();
        java.util.function.Function<LocalDate, Integer> bucketOf = d -> {
            for (int i = 0; i < dated; i++) {
                if (!d.isBefore(buckets.get(i).from()) && !d.isAfter(buckets.get(i).to())) {
                    return i;
                }
            }
            return -1;
        };
        boolean outside = visits.stream().anyMatch(v -> bucketOf.apply(v.arrivalDate()) < 0)
                || turnedAway.stream().anyMatch(t -> bucketOf.apply(LocalDate.ofInstant(t.getAt(), SchoolTimeZone.ZONE)) < 0);
        if (outside) {
            buckets.add(new Bucket("OUTSIDE", "Outside terms", null, null));
        }
        int n = buckets.size();
        java.util.function.Function<LocalDate, Integer> column = d -> {
            int i = bucketOf.apply(d);
            return i < 0 ? n - 1 : i;
        };

        Map<Integer, List<HealthCareVisit>> byBucket = new LinkedHashMap<>();
        for (var v : visits) {
            byBucket.computeIfAbsent(column.apply(v.arrivalDate()), k -> new ArrayList<>()).add(v);
        }
        java.util.function.BiFunction<String, java.util.function.Function<List<HealthCareVisit>, Long>, TrendRow> row =
                (label, f) -> {
                    List<Long> counts = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        counts.add(f.apply(byBucket.getOrDefault(i, List.of())));
                    }
                    return new TrendRow(label, label, false, counts, f.apply(visits));
                };

        long[] turned = new long[n];
        turnedAway.forEach(t -> turned[column.apply(LocalDate.ofInstant(t.getAt(), SchoolTimeZone.ZONE))]++);
        List<Long> turnedCounts = new ArrayList<>();
        for (long c : turned) {
            turnedCounts.add(c);
        }
        List<TrendRow> overview = List.of(
                row.apply("Visits", l -> (long) l.size()),
                row.apply("Students seen", l -> l.stream().map(HealthCareVisit::getStudentId).distinct().count()),
                row.apply("Urgent", l -> l.stream().filter(HealthCareVisit::isUrgent).count()),
                new TrendRow("Turned away", "Turned away", false, turnedCounts, turnedAway.size()));

        List<TrendRow> outcomes = new ArrayList<>();
        for (var o : Outcome.values()) {
            outcomes.add(row.apply(outcomeLabel(o), l -> outcome(l, o)));
        }
        outcomes.add(row.apply("Still open", l -> l.stream().filter(HealthCareVisit::isOpen).count()));

        // Complaints: every one used in the period, most frequent first. A visit with two complaints counts in both.
        Map<String, HealthCareVisit.Complaint> complaintInfo = new LinkedHashMap<>();
        visits.forEach(v -> v.getComplaints().forEach(c -> complaintInfo.putIfAbsent(c.code(), c)));
        List<TrendRow> complaints = complaintInfo.values().stream()
                .map(c -> {
                    var r = row.apply(c.label(), l -> l.stream()
                            .filter(v -> v.getComplaints().stream().anyMatch(k -> k.code().equals(c.code()))).count());
                    return new TrendRow(c.code(), c.label(), c.urgent(), r.counts(), r.total());
                })
                .sorted(Comparator.comparingLong(TrendRow::total).reversed().thenComparing(TrendRow::label))
                .toList();

        Map<String, Boolean> classNames = new java.util.TreeMap<>();
        visits.forEach(v -> classNames.put(className(v), true));
        List<TrendRow> classes = classNames.keySet().stream()
                .map(cls -> row.apply(cls, l -> l.stream().filter(v -> className(v).equals(cls)).count()))
                .sorted(Comparator.comparingLong(TrendRow::total).reversed().thenComparing(TrendRow::label))
                .toList();

        // Medicines given: matched by name ignoring case, shown as first written.
        Map<String, String> medicineName = new LinkedHashMap<>();
        Map<String, Long> medicineTimes = new LinkedHashMap<>();
        Map<String, java.util.Set<String>> medicineStudents = new LinkedHashMap<>();
        for (var v : visits) {
            for (var m : v.getMedications()) {
                String key = m.name().trim().toLowerCase(java.util.Locale.ROOT);
                medicineName.putIfAbsent(key, m.name().trim());
                medicineTimes.merge(key, 1L, Long::sum);
                medicineStudents.computeIfAbsent(key, k -> new java.util.HashSet<>()).add(v.getStudentId());
            }
        }
        List<MedicineRow> medicines = medicineName.entrySet().stream()
                .map(e -> new MedicineRow(e.getValue(), medicineTimes.get(e.getKey()), medicineStudents.get(e.getKey()).size()))
                .sorted(Comparator.comparingLong(MedicineRow::times).reversed().thenComparing(MedicineRow::name))
                .toList();

        // When students come in: by weekday (Monday first) and by hour of arrival.
        List<Count> byWeekday = new ArrayList<>();
        for (var d : java.time.DayOfWeek.values()) {
            long c = visits.stream().filter(v -> v.arrivalDate().getDayOfWeek() == d).count();
            if (c > 0 || d.getValue() <= 5) {
                byWeekday.add(new Count(d.name(), d.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH), c));
            }
        }
        Map<Integer, Long> hours = new java.util.TreeMap<>();
        visits.forEach(v -> hours.merge(v.getArrivedAt().atZone(SchoolTimeZone.ZONE).getHour(), 1L, Long::sum));
        List<Count> byHour = hours.entrySet().stream()
                .map(e -> new Count(String.valueOf(e.getKey()),
                        String.format("%02d:00–%02d:00", e.getKey(), (e.getKey() + 1) % 24), e.getValue()))
                .toList();

        return new Trends(scope.name(), title, from, to, buckets, overview, complaints, outcomes, classes, medicines,
                byWeekday, byHour);
    }

    private static String className(HealthCareVisit v) {
        return v.getStudentClass() == null || v.getStudentClass().isBlank() ? "No class" : v.getStudentClass();
    }

    private List<HealthCareVisit> counted(String schoolId, LocalDate from, LocalDate to) {
        return visitRepo.findBetween(schoolId, startOf(from), startOf(to.plusDays(1))).stream()
                .filter(v -> !v.isVoided()).toList();
    }

    private static List<Count> sorted(java.util.Collection<Count> counts) {
        return counts.stream().sorted(Comparator.comparingLong(Count::count).reversed().thenComparing(Count::label))
                .toList();
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Choose a date range");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("The end date must be on or after the start date");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw new BadRequestException("Choose a range of at most " + MAX_RANGE_DAYS + " days");
        }
    }

    private static Instant startOf(LocalDate day) {
        return day.atStartOfDay(SchoolTimeZone.ZONE).toInstant();
    }

    public static String labels(List<Complaint> complaints) {
        return String.join(", ", complaints.stream().map(Complaint::label).toList());
    }

    public static String outcomeLabel(Outcome outcome) {
        return switch (outcome) {
            case RETURNED_TO_CLASS -> "Returned to class";
            case SENT_HOME -> "Sent home";
            case REFERRED -> "Referred";
        };
    }
}
