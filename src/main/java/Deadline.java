/**
 * Represents a task that must be finished before a given point in time,
 * for example "return book (by: Sunday)".
 *
 * <p>The due date is kept as free text for now; a later increment teaches
 * Floppy to understand real dates.
 */
public class Deadline extends Task {

    /** Type tag shown at the start of a deadline's display form. */
    private static final String TYPE_ICON = "[D]";

    /** When the task is due, exactly as the user wrote it. */
    protected String by;

    /**
     * Constructs a deadline that starts out not done.
     *
     * @param description what the user asked Floppy to remember.
     * @param by          when the task is due.
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
    }

    /**
     * Returns this deadline tagged with its type and due date, for example
     * {@code [D][ ] return book (by: Sunday)}.
     *
     * @return the display form of this deadline.
     */
    @Override
    public String toString() {
        return TYPE_ICON + super.toString() + " (by: " + by + ")";
    }
}
