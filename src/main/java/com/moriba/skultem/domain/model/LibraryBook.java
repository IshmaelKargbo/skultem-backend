package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

/**
 * A title in the school library. {@code totalCopies} is how many the school owns;
 * {@code availableCopies} is how many are on the shelf right now. The difference is the number of
 * copies out on loan, which only {@link #issueCopy()} and {@link #returnCopy()} change.
 */
@Getter
public class LibraryBook extends AggregateRoot<String> {
    private String schoolId;
    private LibraryCategory category;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String shelf;
    private int totalCopies;
    private int availableCopies;

    public LibraryBook(String id, String schoolId, LibraryCategory category, String title, String author,
            String isbn, String publisher, String shelf, int totalCopies, int availableCopies, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.category = category;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.shelf = shelf;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        touch(updatedAt);
    }

    public static LibraryBook create(String schoolId, LibraryCategory category, String title, String author,
            String isbn, String publisher, String shelf, int totalCopies) {
        if (totalCopies < 1) {
            throw new IllegalArgumentException("A book needs at least 1 copy");
        }

        Instant now = Instant.now();
        return new LibraryBook(UUID.randomUUID().toString(), schoolId, category, title, author, isbn, publisher,
                shelf, totalCopies, totalCopies, now, now);
    }

    public int getIssuedCopies() {
        return totalCopies - availableCopies;
    }

    /** Changing the copy count keeps the copies already on loan out on loan. */
    public void update(LibraryCategory category, String title, String author, String isbn, String publisher,
            String shelf, int totalCopies) {
        int issued = getIssuedCopies();

        if (totalCopies < 1) {
            throw new IllegalArgumentException("A book needs at least 1 copy");
        }

        if (totalCopies < issued) {
            throw new IllegalStateException(
                    "Total copies can't be less than the " + issued + " copies currently on loan");
        }

        this.category = category;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.shelf = shelf;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies - issued;
        touch(Instant.now());
    }

    public void issueCopy() {
        if (availableCopies < 1) {
            throw new IllegalStateException("No copies of \"" + title + "\" are available");
        }

        availableCopies -= 1;
        touch(Instant.now());
    }

    public void returnCopy() {
        availableCopies = Math.min(totalCopies, availableCopies + 1);
        touch(Instant.now());
    }
}
