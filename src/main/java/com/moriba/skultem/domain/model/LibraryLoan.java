package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * One copy of a {@link LibraryBook} issued to a student or a teacher.
 * <p>
 * Only ISSUED and RETURNED are stored. Overdue is worked out from the due date when the loan is
 * read ({@link #isOverdue(LocalDate)}), so nothing needs to run on a schedule to mark it.
 * <p>
 * The borrower's name and class are copied in when the book is issued, so the loan history keeps
 * reading correctly after a student moves class or leaves.
 */
@Getter
public class LibraryLoan extends AggregateRoot<String> {

    public enum BorrowerType {
        STUDENT, TEACHER
    }

    public enum Status {
        ISSUED, RETURNED
    }

    private String schoolId;
    private LibraryBook book;
    private BorrowerType borrowerType;
    private String studentId;
    private String teacherId;
    private String borrowerName;
    private String borrowerClass;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Instant returnedAt;
    private Status status;
    private Instant notifiedAt;

    public LibraryLoan(String id, String schoolId, LibraryBook book, BorrowerType borrowerType, String studentId,
            String teacherId, String borrowerName, String borrowerClass, LocalDate issueDate, LocalDate dueDate,
            Instant returnedAt, Status status, Instant notifiedAt, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.book = book;
        this.borrowerType = borrowerType;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.borrowerName = borrowerName;
        this.borrowerClass = borrowerClass;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnedAt = returnedAt;
        this.status = status;
        this.notifiedAt = notifiedAt;
        touch(updatedAt);
    }

    public static LibraryLoan issueToStudent(String schoolId, LibraryBook book, Student student,
            LocalDate today, LocalDate dueDate) {
        return issue(schoolId, book, BorrowerType.STUDENT, student.getId(), null, student.getName(),
                student.getLastClass(), today, dueDate);
    }

    public static LibraryLoan issueToTeacher(String schoolId, LibraryBook book, Teacher teacher,
            LocalDate today, LocalDate dueDate) {
        return issue(schoolId, book, BorrowerType.TEACHER, null, teacher.getId(), teacher.getName(),
                teacher.getDesignation(), today, dueDate);
    }

    private static LibraryLoan issue(String schoolId, LibraryBook book, BorrowerType type, String studentId,
            String teacherId, String name, String borrowerClass, LocalDate today, LocalDate dueDate) {
        // A book is always lent for at least one day.
        if (dueDate == null || !dueDate.isAfter(today)) {
            throw new IllegalArgumentException("Due date must be tomorrow or later");
        }

        book.issueCopy();

        Instant now = Instant.now();
        return new LibraryLoan(UUID.randomUUID().toString(), schoolId, book, type, studentId, teacherId, name,
                borrowerClass, today, dueDate, null, Status.ISSUED, null, now, now);
    }

    public String getBorrowerId() {
        return borrowerType == BorrowerType.TEACHER ? teacherId : studentId;
    }

    public boolean isReturned() {
        return status == Status.RETURNED;
    }

    public boolean isOverdue(LocalDate today) {
        return status == Status.ISSUED && dueDate.isBefore(today);
    }

    public long daysOverdue(LocalDate today) {
        return isOverdue(today) ? ChronoUnit.DAYS.between(dueDate, today) : 0;
    }

    /**
     * Puts the copy back on the shelf. Takes the book as freshly read (and locked) by the caller,
     * so the copy is added to the latest count rather than the one loaded with this loan.
     */
    public void markReturned(LibraryBook currentBook) {
        if (isReturned()) {
            throw new IllegalStateException("This book has already been returned");
        }

        if (!currentBook.getId().equals(book.getId())) {
            throw new IllegalArgumentException("This loan is for a different book");
        }

        this.book = currentBook;
        book.returnCopy();
        status = Status.RETURNED;
        returnedAt = Instant.now();
        touch(returnedAt);
    }

    public void markNotified() {
        notifiedAt = Instant.now();
        touch(notifiedAt);
    }
}
