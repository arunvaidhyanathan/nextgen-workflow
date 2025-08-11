package com.citi.onecms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * Utility class for date and time operations
 */
public final class DateUtils {

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    // Prevent instantiation
    private DateUtils() {
        throw new UnsupportedOperationException("DateUtils class cannot be instantiated");
    }

    // Common date formatters
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(Constants.DATETIME_FORMAT);
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(Constants.TIMESTAMP_FORMAT);
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Time zone
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");
    public static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    /**
     * Get current date and time in UTC
     */
    public static LocalDateTime nowUTC() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * Get current date in UTC
     */
    public static LocalDate todayUTC() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    /**
     * Get current instant
     */
    public static Instant nowInstant() {
        return Instant.now();
    }

    /**
     * Convert LocalDateTime to UTC
     */
    public static LocalDateTime toUTC(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(SYSTEM_ZONE).withZoneSameInstant(DEFAULT_ZONE).toLocalDateTime();
    }

    /**
     * Convert LocalDateTime from UTC to system timezone
     */
    public static LocalDateTime fromUTC(LocalDateTime utcDateTime) {
        if (utcDateTime == null) return null;
        return utcDateTime.atZone(DEFAULT_ZONE).withZoneSameInstant(SYSTEM_ZONE).toLocalDateTime();
    }

    /**
     * Format LocalDateTime to string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        try {
            return dateTime.format(DATETIME_FORMATTER);
        } catch (Exception e) {
            logger.error("Error formatting datetime: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Format LocalDate to string
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        try {
            return date.format(DATE_FORMATTER);
        } catch (Exception e) {
            logger.error("Error formatting date: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Format LocalDateTime with custom pattern
     */
    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return dateTime.format(formatter);
        } catch (Exception e) {
            logger.error("Error formatting datetime with pattern '{}': {}", pattern, e.getMessage());
            return null;
        }
    }

    /**
     * Parse string to LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeString, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.error("Error parsing datetime '{}': {}", dateTimeString, e.getMessage());
            return null;
        }
    }

    /**
     * Parse string to LocalDate
     */
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date '{}': {}", dateString, e.getMessage());
            return null;
        }
    }

    /**
     * Parse string to LocalDateTime with custom pattern
     */
    public static LocalDateTime parseDateTime(String dateTimeString, String pattern) {
        if (dateTimeString == null || pattern == null) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.parse(dateTimeString, formatter);
        } catch (DateTimeParseException e) {
            logger.error("Error parsing datetime '{}' with pattern '{}': {}", dateTimeString, pattern, e.getMessage());
            return null;
        }
    }

    /**
     * Convert LocalDateTime to Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
    }

    /**
     * Convert Date to LocalDateTime
     */
    public static LocalDateTime fromDate(Date date) {
        if (date == null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE);
    }

    /**
     * Calculate age in years
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Calculate duration between two dates in days
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Calculate duration between two datetimes in hours
     */
    public static long hoursBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) return 0;
        return ChronoUnit.HOURS.between(startDateTime, endDateTime);
    }

    /**
     * Calculate duration between two datetimes in minutes
     */
    public static long minutesBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) return 0;
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime);
    }

    /**
     * Calculate business days between two dates (excludes weekends)
     */
    public static long businessDaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return 0;
        
        long businessDays = 0;
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                businessDays++;
            }
            current = current.plusDays(1);
        }
        
        return businessDays;
    }

    /**
     * Add business days to a date (excludes weekends)
     */
    public static LocalDate addBusinessDays(LocalDate date, int businessDays) {
        if (date == null || businessDays <= 0) return date;
        
        LocalDate result = date;
        int addedDays = 0;
        
        while (addedDays < businessDays) {
            result = result.plusDays(1);
            DayOfWeek dayOfWeek = result.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                addedDays++;
            }
        }
        
        return result;
    }

    /**
     * Check if a date is a weekend
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Check if a date is today
     */
    public static boolean isToday(LocalDate date) {
        if (date == null) return false;
        return date.equals(LocalDate.now());
    }

    /**
     * Check if a date is in the past
     */
    public static boolean isPast(LocalDate date) {
        if (date == null) return false;
        return date.isBefore(LocalDate.now());
    }

    /**
     * Check if a date is in the future
     */
    public static boolean isFuture(LocalDate date) {
        if (date == null) return false;
        return date.isAfter(LocalDate.now());
    }

    /**
     * Get start of day
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        if (date == null) return null;
        return date.atStartOfDay();
    }

    /**
     * Get end of day
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        if (date == null) return null;
        return date.atTime(23, 59, 59, 999999999);
    }

    /**
     * Get start of month
     */
    public static LocalDate startOfMonth(LocalDate date) {
        if (date == null) return null;
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Get end of month
     */
    public static LocalDate endOfMonth(LocalDate date) {
        if (date == null) return null;
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Get start of year
     */
    public static LocalDate startOfYear(LocalDate date) {
        if (date == null) return null;
        return date.with(TemporalAdjusters.firstDayOfYear());
    }

    /**
     * Get end of year
     */
    public static LocalDate endOfYear(LocalDate date) {
        if (date == null) return null;
        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    /**
     * Get next working day (excludes weekends)
     */
    public static LocalDate nextWorkingDay(LocalDate date) {
        if (date == null) return null;
        
        LocalDate nextDay = date.plusDays(1);
        while (isWeekend(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }

    /**
     * Get previous working day (excludes weekends)
     */
    public static LocalDate previousWorkingDay(LocalDate date) {
        if (date == null) return null;
        
        LocalDate previousDay = date.minusDays(1);
        while (isWeekend(previousDay)) {
            previousDay = previousDay.minusDays(1);
        }
        return previousDay;
    }

    /**
     * Calculate SLA due date based on priority
     */
    public static LocalDateTime calculateSLADueDate(LocalDateTime startDate, String priority) {
        if (startDate == null || priority == null) return null;
        
        int hoursToAdd;
        switch (priority.toUpperCase()) {
            case Constants.PRIORITY_CRITICAL:
                hoursToAdd = Constants.SLA_CRITICAL_HOURS;
                break;
            case Constants.PRIORITY_HIGH:
                hoursToAdd = Constants.SLA_HIGH_HOURS;
                break;
            case Constants.PRIORITY_MEDIUM:
                hoursToAdd = Constants.SLA_MEDIUM_HOURS;
                break;
            case Constants.PRIORITY_LOW:
                hoursToAdd = Constants.SLA_LOW_HOURS;
                break;
            default:
                hoursToAdd = Constants.SLA_MEDIUM_HOURS; // Default to medium
        }
        
        return startDate.plusHours(hoursToAdd);
    }

    /**
     * Check if SLA is breached
     */
    public static boolean isSLABreached(LocalDateTime startDate, LocalDateTime currentDate, String priority) {
        if (startDate == null || currentDate == null || priority == null) return false;
        
        LocalDateTime slaDeadline = calculateSLADueDate(startDate, priority);
        return currentDate.isAfter(slaDeadline);
    }

    /**
     * Get remaining time until SLA deadline
     */
    public static Duration getRemainingTimeToSLA(LocalDateTime startDate, String priority) {
        if (startDate == null || priority == null) return Duration.ZERO;
        
        LocalDateTime slaDeadline = calculateSLADueDate(startDate, priority);
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(slaDeadline)) {
            return Duration.ZERO; // SLA already breached
        }
        
        return Duration.between(now, slaDeadline);
    }

    /**
     * Format duration to human-readable string
     */
    public static String formatDuration(Duration duration) {
        if (duration == null) return null;
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(" day").append(days > 1 ? "s" : "");
        }
        
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(" hour").append(hours > 1 ? "s" : "");
        }
        
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        }
        
        if (sb.length() == 0) {
            return "less than a minute";
        }
        
        return sb.toString();
    }

    /**
     * Get current fiscal year based on calendar year
     */
    public static int getCurrentFiscalYear() {
        return LocalDate.now().getYear();
    }

    /**
     * Get current quarter
     */
    public static int getCurrentQuarter() {
        int month = LocalDate.now().getMonthValue();
        return (month - 1) / 3 + 1;
    }

    /**
     * Check if two date ranges overlap
     */
    public static boolean dateRangesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) return false;
        return !end1.isBefore(start2) && !end2.isBefore(start1);
    }

    /**
     * Get timestamp for file naming
     */
    public static String getTimestampForFileName() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    /**
     * Validate date range
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return false;
        return !startDate.isAfter(endDate);
    }

    /**
     * Get week number of year
     */
    public static int getWeekOfYear(LocalDate date) {
        if (date == null) return 0;
        return date.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
    }
}