package floppy;

import java.nio.file.Path;
import java.util.List;

import floppy.task.Deadline;
import floppy.task.Event;
import floppy.task.Task;
import floppy.task.Todo;

/**
 * Floppy is a command line chatbot with the personality of a 1.44 MB floppy disk:
 * it whirrs, it clicks, and it is delighted to be useful again after decades in a drawer.
 *
 * <p>At this stage (Level-7) Floppy tracks three kinds of task -- todos, deadlines
 * and events -- lists them on demand, records which ones are done, deletes them when
 * asked, and saves them to disk so they survive a restart. It exits on {@code bye}.
 */
public class Floppy {

    /** The command that makes Floppy exit. */
    private static final String COMMAND_EXIT = "bye";

    /** The command that makes Floppy print every task it is holding. */
    private static final String COMMAND_LIST = "list";

    /** The command that marks a task as done. */
    private static final String COMMAND_MARK = "mark";

    /** The command that marks a task as not done. */
    private static final String COMMAND_UNMARK = "unmark";

    /** The command that removes a task from the list. */
    private static final String COMMAND_DELETE = "delete";

    /** The command that adds a task with no date or time. */
    private static final String COMMAND_TODO = "todo";

    /** The command that adds a task due by a given time. */
    private static final String COMMAND_DEADLINE = "deadline";

    /** The command that adds a task spanning a start and an end time. */
    private static final String COMMAND_EVENT = "event";

    /** Regular expression matching a whole number, with an optional leading sign. */
    private static final String WHOLE_NUMBER_REGEX = "[+-]?\\d+";

    /** Stands in for a task number too large to hold in an int; never names a real task. */
    private static final int TASK_NUMBER_TOO_LARGE = -1;

    /** Regular expression matching the whitespace that separates words in a command. */
    private static final String WHITESPACE_REGEX = "\\s+";

    /** Separates a deadline's description from its due time. */
    private static final String MARKER_BY = "/by";

    /** Separates an event's description from its start time. */
    private static final String MARKER_FROM = "/from";

    /** Separates an event's start time from its end time. */
    private static final String MARKER_TO = "/to";

    /**
     * Where Floppy keeps its tasks between runs. The path is relative, so it resolves
     * against the folder Floppy is run from, and it is built from separate parts so
     * that the right folder separator is used on every operating system.
     */
    private static final Path DATA_FILE = Path.of("data", "floppy.txt");

    /**
     * Runs the chatbot: greets the user, then reads and handles one command per line
     * until the user types {@code bye} or the input runs out.
     *
     * @param args command line arguments, which Floppy does not use.
     */
    public static void main(String[] args) {
        Ui ui = new Ui();
        ui.showGreeting();

        Storage storage = new Storage(DATA_FILE);
        TaskList tasks = loadTasks(storage, ui);

        // hasNextCommand() is false once the input runs out, which happens when
        // input is piped in from a file rather than typed.
        while (ui.hasNextCommand()) {
            String input = ui.readCommand();

            if (input.equalsIgnoreCase(COMMAND_EXIT)) {
                break;
            }

            try {
                handleCommand(tasks, input, ui);
                storage.save(tasks.asList());
            } catch (FloppyException e) {
                ui.showProblem(e.getMessage());
            }
        }

        ui.showFarewell();
    }

    /**
     * Loads the saved tasks and returns them. Warns the user about any unreadable lines
     * that were skipped, and reports the problem and starts with no tasks if the data file
     * cannot be read at all.
     *
     * @param storage where the tasks were saved.
     * @param ui      where messages are shown.
     * @return the tasks to start with.
     */
    private static TaskList loadTasks(Storage storage, Ui ui) {
        List<Task> savedTasks;
        try {
            savedTasks = storage.load();
        } catch (FloppyException e) {
            ui.showProblem(e.getMessage());
            return new TaskList();
        }

        List<Integer> skippedLineNumbers = storage.getSkippedLineNumbers();
        if (!skippedLineNumbers.isEmpty()) {
            ui.showProblem("Some of " + DATA_FILE + " was unreadable (line numbers " + skippedLineNumbers
                    + "), so I skipped those lines. The original is backed up at "
                    + storage.getBackupPath() + ".");
        }

        if (!savedTasks.isEmpty()) {
            ui.showTasksLoaded(savedTasks.size());
        }
        return new TaskList(savedTasks);
    }

    /**
     * Carries out one command from the user.
     *
     * @param tasks the tasks Floppy is holding.
     * @param input the whole line the user entered, already stripped.
     * @param ui    where messages are shown.
     * @throws FloppyException if the command cannot be carried out as typed.
     */
    private static void handleCommand(TaskList tasks, String input, Ui ui) throws FloppyException {
        if (input.isEmpty()) {
            ui.showBlankInput();
        } else if (isCommand(input, COMMAND_LIST)) {
            listTasks(tasks, input, ui);
        } else if (isCommand(input, COMMAND_MARK)) {
            changeTaskStatus(tasks, input, true, ui);
        } else if (isCommand(input, COMMAND_UNMARK)) {
            changeTaskStatus(tasks, input, false, ui);
        } else if (isCommand(input, COMMAND_DELETE)) {
            deleteTask(tasks, input, ui);
        } else if (isCommand(input, COMMAND_TODO)) {
            addTask(tasks, createTodo(input), ui);
        } else if (isCommand(input, COMMAND_DEADLINE)) {
            addTask(tasks, createDeadline(input), ui);
        } else if (isCommand(input, COMMAND_EVENT)) {
            addTask(tasks, createEvent(input), ui);
        } else {
            throw new FloppyException("'" + commandWordOf(input) + "'? That's not in my directory. "
                    + "I know todo, deadline, event, list, mark, unmark, delete and bye.");
        }
    }

    /**
     * Returns whether the user's input starts with the given command word.
     *
     * @param input       the whole line the user entered, already stripped.
     * @param commandWord the command word to look for.
     * @return true if the first word of the input is that command word.
     */
    private static boolean isCommand(String input, String commandWord) {
        return commandWordOf(input).equalsIgnoreCase(commandWord);
    }

    /**
     * Returns the first word of the user's input, which names the command.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the command word, or an empty string if the input was empty.
     */
    private static String commandWordOf(String input) {
        return input.split(WHITESPACE_REGEX, 2)[0];
    }

    /**
     * Splits text at the first occurrence of a marker such as {@value #MARKER_BY},
     * discarding any whitespace around it.
     *
     * @param text   the text to split.
     * @param marker the marker to split at.
     * @return the text before the marker, followed by the text after it if present.
     */
    private static String[] splitAtMarker(String text, String marker) {
        return text.split("\\s*" + marker + "\\s*", 2);
    }

    /**
     * Marks the task the user picked as done or not done, then reports the outcome.
     *
     * @param tasks        the tasks Floppy is holding.
     * @param input        the whole line the user entered, already stripped.
     * @param shouldBeDone true to mark the task done, false to mark it not done.
     * @param ui           where messages are shown.
     * @throws FloppyException if the command does not name a task that exists.
     */
    private static void changeTaskStatus(TaskList tasks, String input, boolean shouldBeDone, Ui ui)
            throws FloppyException {
        Task task = findTask(tasks, input);

        if (shouldBeDone) {
            task.markAsDone();
            ui.showTaskMarked(task);
        } else {
            task.markAsNotDone();
            ui.showTaskUnmarked(task);
        }
    }

    /**
     * Removes the task the user picked from the list, then reports what was removed.
     *
     * @param tasks the tasks Floppy is holding.
     * @param input the whole line the user entered, already stripped.
     * @param ui    where messages are shown.
     * @throws FloppyException if the command does not name a task that exists.
     */
    private static void deleteTask(TaskList tasks, String input, Ui ui) throws FloppyException {
        Task task = findTask(tasks, input);
        tasks.remove(task);
        ui.showTaskDeleted(task, tasks.size());
    }

    /**
     * Returns the task named by the number in the user's command.
     *
     * @param tasks the tasks Floppy is holding.
     * @param input the whole line the user entered, already stripped.
     * @return the task the user picked.
     * @throws FloppyException if the number is missing, not a number, or names no stored task.
     */
    private static Task findTask(TaskList tasks, String input) throws FloppyException {
        String argument = argumentOf(input);

        if (argument.isEmpty()) {
            throw new FloppyException("Which one? Give me a number, e.g. '"
                    + commandWordOf(input) + " 2'.");
        }

        if (!argument.matches(WHOLE_NUMBER_REGEX)) {
            throw new FloppyException("'" + argument
                    + "' is not a number, and I only speak in sectors.");
        }

        if (tasks.isEmpty()) {
            throw new FloppyException("There's nothing on me to " + commandWordOf(input).toLowerCase()
                    + " yet. Add a task first, e.g. 'todo borrow book'.");
        }

        int taskNumber = parseTaskNumber(argument);

        if (taskNumber < 1 || taskNumber > tasks.size()) {
            throw new FloppyException("I have " + Ui.describeCount(tasks.size())
                    + ". There is nothing at number " + argument + ".");
        }

        return tasks.get(taskNumber - 1);
    }

    /**
     * Returns the task number written in the given text.
     *
     * @param text a whole number, already checked against {@value #WHOLE_NUMBER_REGEX}.
     * @return the number, or {@value #TASK_NUMBER_TOO_LARGE} if it will not fit in an int.
     */
    private static int parseTaskNumber(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            // Too many digits for an int, so no stored task can carry this number.
            return TASK_NUMBER_TOO_LARGE;
        }
    }

    /**
     * Stores a task and reports it to the user.
     *
     * @param tasks the tasks Floppy is holding.
     * @param task  the task to store.
     * @param ui    where messages are shown.
     */
    private static void addTask(TaskList tasks, Task task, Ui ui) {
        tasks.add(task);
        ui.showTaskAdded(task, tasks.size());
    }

    /**
     * Returns everything the user typed after the command word.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the argument text, or an empty string if the command had no argument.
     */
    private static String argumentOf(String input) {
        String[] parts = input.split(WHITESPACE_REGEX, 2);
        return parts.length < 2 ? "" : parts[1].strip();
    }

    /**
     * Builds a todo from the user's input.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the new todo.
     * @throws FloppyException if the description was missing.
     */
    private static Todo createTodo(String input) throws FloppyException {
        String description = argumentOf(input);

        if (description.isEmpty()) {
            throw new FloppyException("A todo needs a description, e.g. 'todo borrow book'.");
        }
        return new Todo(description);
    }

    /**
     * Builds a deadline from the user's input, splitting it at the {@value #MARKER_BY} marker.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the new deadline.
     * @throws FloppyException if the description or the due time was missing.
     */
    private static Deadline createDeadline(String input) throws FloppyException {
        String[] parts = splitAtMarker(argumentOf(input), MARKER_BY);
        String description = parts[0].strip();
        String by = parts.length < 2 ? "" : parts[1].strip();

        if (description.isEmpty() || by.isEmpty()) {
            throw new FloppyException("A deadline needs a description and a time, e.g. "
                    + "'deadline return book " + MARKER_BY + " Sunday'.");
        }
        return new Deadline(description, by);
    }

    /**
     * Builds an event from the user's input, splitting it at the {@value #MARKER_FROM}
     * and {@value #MARKER_TO} markers.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the new event.
     * @throws FloppyException if the description, start or end was missing.
     */
    private static Event createEvent(String input) throws FloppyException {
        String[] fromParts = splitAtMarker(argumentOf(input), MARKER_FROM);
        String description = fromParts[0].strip();
        String from = "";
        String to = "";

        if (fromParts.length == 2) {
            String[] toParts = splitAtMarker(fromParts[1], MARKER_TO);
            from = toParts[0].strip();
            to = toParts.length < 2 ? "" : toParts[1].strip();
        }

        if (description.isEmpty() || from.isEmpty() || to.isEmpty()) {
            throw new FloppyException("An event needs a description, a start and an end, e.g. "
                    + "'event project meeting " + MARKER_FROM + " Mon 2pm " + MARKER_TO + " 4pm'.");
        }
        return new Event(description, from, to);
    }

    /**
     * Shows every task Floppy is holding, or reports a problem if the list command
     * was given something after it.
     *
     * @param tasks the tasks Floppy is holding.
     * @param input the whole line the user entered, already stripped.
     * @param ui    where the tasks are shown.
     * @throws FloppyException if the list command was given something after it.
     */
    private static void listTasks(TaskList tasks, String input, Ui ui) throws FloppyException {
        String argument = argumentOf(input);

        if (!argument.isEmpty()) {
            throw new FloppyException("'" + COMMAND_LIST + "' takes nothing after it. "
                    + "Drop the '" + argument + "' and I'll read the whole disk.");
        }

        ui.showTasks(tasks.asList());
    }
}
