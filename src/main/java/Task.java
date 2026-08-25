/**
 * Represents a single item that Floppy is keeping track of, together with
 * whether the user has finished it.
 *
 * <p>The fields are {@code protected} rather than {@code private} so that later
 * increments can introduce specialised kinds of tasks that inherit from this class.
 */
public class Task {

    /** What the user asked Floppy to remember. */
    protected String description;

    /** Whether the user has marked this task as finished. */
    protected boolean isDone;

    /**
     * Constructs a task that starts out not done.
     *
     * @param description what the user asked Floppy to remember.
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /** Marks this task as finished. */
    public void markAsDone() {
        this.isDone = true;
    }

    /** Marks this task as not finished, undoing a previous {@link #markAsDone()}. */
    public void markAsNotDone() {
        this.isDone = false;
    }

    /**
     * Returns the single character shown inside the status brackets.
     *
     * @return "X" if this task is done, a space otherwise.
     */
    public String getStatusIcon() {
        return isDone ? "X" : " ";
    }

    /**
     * Returns this task rendered as a status box followed by its description,
     * for example {@code [X] read book}.
     *
     * @return the display form of this task.
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}
