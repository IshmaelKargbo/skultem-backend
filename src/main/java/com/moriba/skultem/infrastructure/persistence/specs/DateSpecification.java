package com.moriba.skultem.infrastructure.persistence.specs;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.jpa.domain.Specification;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.persistence.criteria.Expression;

public class DateSpecification {

    // ── Entry point ──────────────────────────────────────────────────────────

    public static <T> Specification<T> build(Filter filter) {
        return switch (filter.type().toLowerCase()) {
            case "date"    -> buildLocalDate(filter.field(), filter.operator(), filter.value(), filter.valueTo());
            case "instant" -> buildInstant(filter.field(), filter.operator(), filter.value(), filter.valueTo());
            case "age"     -> buildAge(filter.field(), filter.operator(), filter.value(), filter.valueTo());
            default        -> throw new IllegalArgumentException("Unsupported date type: " + filter.type());
        };
    }

    // ── LocalDate ────────────────────────────────────────────────────────────

    private static <T> Specification<T> buildLocalDate(
            String field, FilterOperator operator, String value, String valueTo) {

        return (root, query, cb) -> {
            Expression<LocalDate> column = PathResolver.getPath(root, field);

            LocalDate v  = parseDate(value);
            LocalDate v2 = parseDate(valueTo);

            return switch (operator) {
                case EQUALS                -> cb.equal(column, v);
                case NOT_EQUALS            -> cb.notEqual(column, v);
                case GREATER_THAN          -> cb.greaterThan(column, v);
                case GREATER_THAN_OR_EQUAL -> cb.greaterThanOrEqualTo(column, v);
                case LESS_THAN             -> cb.lessThan(column, v);
                case LESS_THAN_OR_EQUAL    -> cb.lessThanOrEqualTo(column, v);
                case BETWEEN -> {
                    if (v == null || v2 == null)
                        throw new IllegalArgumentException("BETWEEN requires both value and valueTo");
                    yield cb.between(column, v, v2);
                }
                default -> null;
            };
        };
    }

    // ── Instant ──────────────────────────────────────────────────────────────

    private static <T> Specification<T> buildInstant(
            String field, FilterOperator operator, String value, String valueTo) {

        return (root, query, cb) -> {
            Expression<Instant> column = PathResolver.getPath(root, field);

            Instant startOfDay  = toStartOfDay(value);
            Instant endOfDay    = toEndOfDay(value);
            Instant endOfDay2   = toEndOfDay(valueTo);

            return switch (operator) {
                case EQUALS                -> cb.between(column.as(Instant.class), startOfDay, endOfDay);
                case NOT_EQUALS            -> cb.not(cb.between(column.as(Instant.class), startOfDay, endOfDay));
                case GREATER_THAN          -> cb.greaterThan(column.as(Instant.class), endOfDay);
                case GREATER_THAN_OR_EQUAL -> cb.greaterThanOrEqualTo(column.as(Instant.class), startOfDay);
                case LESS_THAN             -> cb.lessThan(column.as(Instant.class), startOfDay);
                case LESS_THAN_OR_EQUAL    -> cb.lessThanOrEqualTo(column.as(Instant.class), endOfDay);
                case BETWEEN -> {
                    if (startOfDay == null || endOfDay2 == null)
                        throw new IllegalArgumentException("BETWEEN requires both value and valueTo");
                    yield cb.between(column.as(Instant.class), startOfDay, endOfDay2);
                }
                default -> null;
            };
        };
    }

    // ── Age ──────────────────────────────────────────────────────────────────

    private static <T> Specification<T> buildAge(
            String field, FilterOperator operator, String value, String valueTo) {

        int age  = parseAge(value);
        int age2 = parseAge(valueTo);

        LocalDate today = LocalDate.now();

        return switch (operator) {

            case EQUALS -> {
                LocalDate from = today.minusYears(age + 1).plusDays(1);
                LocalDate to   = today.minusYears(age);
                yield buildLocalDate(field, FilterOperator.BETWEEN, from.toString(), to.toString());
            }

            case NOT_EQUALS -> {
                LocalDate from = today.minusYears(age + 1).plusDays(1);
                LocalDate to   = today.minusYears(age);
                yield Specification.not(buildLocalDate(field, FilterOperator.BETWEEN, from.toString(), to.toString()));
            }

            case GREATER_THAN -> {
                // older than X → DOB strictly before today minus X years
                LocalDate before = today.minusYears(age);
                yield buildLocalDate(field, FilterOperator.LESS_THAN, before.toString(), null);
            }

            case GREATER_THAN_OR_EQUAL -> {
                // age >= X → DOB on or before today minus X years
                LocalDate on = today.minusYears(age);
                yield buildLocalDate(field, FilterOperator.LESS_THAN_OR_EQUAL, on.toString(), null);
            }

            case LESS_THAN -> {
                // younger than X → DOB strictly after today minus X years
                LocalDate after = today.minusYears(age);
                yield buildLocalDate(field, FilterOperator.GREATER_THAN, after.toString(), null);
            }

            case LESS_THAN_OR_EQUAL -> {
                // age <= X → DOB on or after today minus X years
                LocalDate on = today.minusYears(age);
                yield buildLocalDate(field, FilterOperator.GREATER_THAN_OR_EQUAL, on.toString(), null);
            }

            case BETWEEN -> {
                if (age2 == 0)
                    throw new IllegalArgumentException("BETWEEN requires valueTo for age filter");
                // age between X and Y → DOB between (today - Y - 1 + 1day) and (today - X)
                LocalDate from = today.minusYears(age2 + 1).plusDays(1);
                LocalDate to   = today.minusYears(age);
                yield buildLocalDate(field, FilterOperator.BETWEEN, from.toString(), to.toString());
            }

            default -> throw new IllegalArgumentException("Unsupported operator for AGE: " + operator);
        };
    }

    // ── Public age helpers (for programmatic use) ────────────────────────────

    public static <T> Specification<T> ageEquals(String dobField, int age) {
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusYears(age + 1).plusDays(1);
        LocalDate to    = today.minusYears(age);
        return buildLocalDate(dobField, FilterOperator.BETWEEN, from.toString(), to.toString());
    }

    public static <T> Specification<T> ageBetween(String dobField, int minAge, int maxAge) {
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusYears(maxAge + 1).plusDays(1);
        LocalDate to    = today.minusYears(minAge);
        return buildLocalDate(dobField, FilterOperator.BETWEEN, from.toString(), to.toString());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.trim());
    }

    private static int parseAge(String value) {
        if (value == null || value.isBlank()) return 0;
        return Integer.parseInt(value.trim());
    }

    private static Instant toStartOfDay(String value) {
        LocalDate date = parseDate(value);
        return date != null ? date.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
    }

    private static Instant toEndOfDay(String value) {
        LocalDate date = parseDate(value);
        return date != null ? date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC) : null;
    }
}