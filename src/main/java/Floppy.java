import java.util.Scanner;

/**
 * Floppy is a command line chatbot with the personality of a 1.44 MB floppy disk:
 * it whirrs, it clicks, and it is delighted to be useful again after decades in a drawer.
 *
 * <p>At this stage (Level-4) Floppy tracks three kinds of task -- todos, deadlines
 * and events -- lists them on demand, and records which ones are done.
 * It exits on {@code bye}.
 */
public class Floppy {

    /** Divider printed above and below every block of Floppy's output. */
    private static final String HORIZONTAL_LINE =
            "    ____________________________________________________________";

    /** Indentation placed in front of every line Floppy speaks. */
    private static final String INDENT = "     ";

    /** Deeper indentation used when a line shows the detail of the line above it. */
    private static final String INDENT_DETAIL = INDENT + "  ";

    /** ASCII art shown once at startup. */
    private static final String BANNER =
            " _____ _                      \n"
            + "|  ___| | ___  _ __  _ __  _   _\n"
            + "| |_  | |/ _ \\| '_ \\| '_ \\| | | |\n"
            + "|  _| | | (_) | |_) | |_) | |_| |\n"
            + "|_|   |_|\\___/| .__/| .__/ \\__, |\n"
            + "              |_|   |_|    |___/";

    /** The command that makes Floppy exit. */
    private static final String COMMAND_EXIT = "bye";

    /** The command that makes Floppy print every task it is holding. */
    private static final String COMMAND_LIST = "list";

    /** The command that marks a task as done. */
    private static final String COMMAND_MARK = "mark";

    /** The command that marks a task as not done. */
    private static final String COMMAND_UNMARK = "unmark";

    /** The command that adds a task with no date or time. */
    private static final String COMMAND_TODO = "todo";

    /** The command that adds a task due by a given time. */
    private static final String COMMAND_DEADLINE = "deadline";

    /** The command that adds a task spanning a start and an end time. */
    private static final String COMMAND_EVENT = "event";

    /** Separates a deadline's description from its due time. */
    private static final String MARKER_BY = "/by";

    /** Separates an event's description from its start time. */
    private static final String MARKER_FROM = "/from";

    /** Separates an event's start time from its end time. */
    private static final String MARKER_TO = "/to";

    /**
     * Largest number of tasks Floppy can hold. The project brief allows us to assume
     * the user never exceeds this, so a fixed-size array is enough for now.
     */
    private static final int MAX_TASKS = 100;

    /**
     * Drive noises used to introduce each stored task. Floppy cycles through them
     * so that repeated commands do not produce identical replies, which makes the
     * chatbot feel more alive than a single fixed prefix would.
     */
    private static final String[] DRIVE_NOISES = {
        "*whirr-click*",
        "*chk-chk-chk*",
        "*seeking track 00*",
        "*clunk... spinning up*"
    };

    /**
     * Runs the chatbot: greets the user, then reads and handles one command per line
     * until the user types {@code bye} or the input runs out.
     *
     * @param args command line arguments, which Floppy does not use.
     */
    public static void main(String[] args) {
        printGreeting();

        Scanner in = new Scanner(System.in);
        Task[] tasks = new Task[MAX_TASKS];
        int taskCount = 0;

        // hasNextLine() guards against the input stream ending without a "bye",
        // which happens when input is piped in from a file rather than typed.
        while (in.hasNextLine()) {
            String input = in.nextLine().strip();

            if (input.equalsIgnoreCase(COMMAND_EXIT)) {
                break;
            }

            taskCount = handleCommand(tasks, taskCount, input);
        }

        printFarewell();
    }

    /**
     * Carries out one command from the user and returns the resulting task count.
     * Reports the problem to the user if the command word is not recognised.
     *
     * @param tasks     the storage array.
     * @param taskCount how many slots are in use before the command runs.
     * @param input     the whole line the user entered, already stripped.
     * @return how many slots are in use after the command has run.
     */
    private static int handleCommand(Task[] tasks, int taskCount, String input) {
        if (input.isEmpty()) {
            printBlankInputResponse();
        } else if (input.equalsIgnoreCase(COMMAND_LIST)) {
            printTasks(tasks, taskCount);
        } else if (isCommand(input, COMMAND_MARK)) {
            changeTaskStatus(tasks, taskCount, input, true);
        } else if (isCommand(input, COMMAND_UNMARK)) {
            changeTaskStatus(tasks, taskCount, input, false);
        } else if (isCommand(input, COMMAND_TODO)) {
            return addTask(tasks, taskCount, createTodo(input));
        } else if (isCommand(input, COMMAND_DEADLINE)) {
            return addTask(tasks, taskCount, createDeadline(input));
        } else if (isCommand(input, COMMAND_EVENT)) {
            return addTask(tasks, taskCount, createEvent(input));
        } else {
            printUnknownCommandResponse(input);
        }

        return taskCount;
    }

    /**
     * Returns whether the user's input starts with the given command word.
     *
     * @param input       the whole line the user entered, already stripped.
     * @param commandWord the command word to look for.
     * @return true if the first word of the input is that command word.
     */
    private static boolean isCommand(String input, String commandWord) {
        String firstWord = input.split("\\s+")[0];
        return firstWord.equalsIgnoreCase(commandWord);
    }

    /**
     * Marks the task the user picked as done or not done, then reports the outcome.
     * Does nothing if the command did not identify a task.
     *
     * @param tasks         the storage array.
     * @param taskCount     how many slots are actually in use.
     * @param input         the whole line the user entered, already stripped.
     * @param shouldBeDone  true to mark the task done, false to mark it not done.
     */
    private static void changeTaskStatus(Task[] tasks, int taskCount, String input, boolean shouldBeDone) {
        Task task = findTask(tasks, taskCount, input);

        if (task == null) {
            return;
        }

        if (shouldBeDone) {
            task.markAsDone();
            printStatusChanged("*clack* Nice! I've marked this task as done:", task);
        } else {
            task.markAsNotDone();
            printStatusChanged("*rewinds* OK, I've marked this task as not done yet:", task);
        }
    }

    /**
     * Returns the task named by the number in the user's command.
     * Reports the problem and returns null if that number is missing, not a number,
     * or outside the range of stored tasks.
     *
     * @param tasks     the storage array.
     * @param taskCount how many slots are actually in use.
     * @param input     the whole line the user entered, already stripped.
     * @return the task the user picked, or null if the command did not identify one.
     */
    private static Task findTask(Task[] tasks, int taskCount, String input) {
        String[] parts = input.split("\\s+", 2);

        if (parts.length < 2) {
            printProblem("Which one? Give me a number, e.g. '" + parts[0] + " 2'.");
            return null;
        }

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            printProblem("'" + parts[1] + "' is not a number, and I only speak in sectors.");
            return null;
        }

        if (taskNumber < 1 || taskNumber > taskCount) {
            printProblem("I have " + describeCount(taskCount)
                    + ". There is nothing at number " + taskNumber + ".");
            return null;
        }

        return tasks[taskNumber - 1];
    }

    /** Prints the banner and welcome message shown when Floppy starts up. */
    private static void printGreeting() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(BANNER);
        System.out.println(INDENT + "*click... whirr... clunk*");
        System.out.println(INDENT + "Hello! I'm Floppy, 1.44 MB of pure determination.");
        System.out.println(INDENT + "Tell me a task and I'll hold onto it:");
        System.out.println(INDENT_DETAIL + "todo borrow book");
        System.out.println(INDENT_DETAIL + "deadline return book /by Sunday");
        System.out.println(INDENT_DETAIL + "event project meeting /from Mon 2pm /to 4pm");
        System.out.println(INDENT + "Then 'list', 'mark 1', 'unmark 1', or 'bye'.");
        System.out.println(INDENT + "What can I do for you?");
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Confirms that a task has been stored, and says how many tasks are now held.
     *
     * @param task      the task that was just stored.
     * @param taskIndex position the task was stored at, used to pick the drive noise.
     * @param taskCount how many tasks Floppy holds after this one was added.
     */
    private static void printTaskAdded(Task task, int taskIndex, int taskCount) {
        String noise = DRIVE_NOISES[taskIndex % DRIVE_NOISES.length];
        System.out.println(HORIZONTAL_LINE);
        System.out.println(INDENT + noise + " Got it. I've added this task:");
        System.out.println(INDENT_DETAIL + task);
        System.out.println(INDENT + "Now you have " + describeCount(taskCount) + " in the list.");
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Returns a task count with the right singular or plural noun,
     * for example "1 task" or "5 tasks".
     *
     * @param taskCount the number of tasks to describe.
     * @return the count followed by the correctly pluralised word "task".
     */
    private static String describeCount(int taskCount) {
        String noun = taskCount == 1 ? " task" : " tasks";
        return taskCount + noun;
    }

    /**
     * Stores a task, reports it to the user, and returns the updated task count.
     * Does nothing if the task could not be built or if storage is full.
     *
     * @param tasks     the storage array.
     * @param taskCount how many slots are in use before this call.
     * @param task      the task to store, or null if the command was malformed.
     * @return how many slots are in use after this call.
     */
    private static int addTask(Task[] tasks, int taskCount, Task task) {
        if (task == null) {
            return taskCount;
        }
        if (taskCount == MAX_TASKS) {
            printDiskFullResponse();
            return taskCount;
        }

        tasks[taskCount] = task;
        printTaskAdded(task, taskCount, taskCount + 1);
        return taskCount + 1;
    }

    /**
     * Returns everything the user typed after the command word.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the argument text, or an empty string if the command had no argument.
     */
    private static String argumentOf(String input) {
        String[] parts = input.split("\\s+", 2);
        return parts.length < 2 ? "" : parts[1].strip();
    }

    /**
     * Builds a todo from the user's input.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the new todo, or null if the description was missing.
     */
    private static Todo createTodo(String input) {
        String description = argumentOf(input);

        if (description.isEmpty()) {
            printProblem("A todo needs a description, e.g. 'todo borrow book'.");
            return null;
        }
        return new Todo(description);
    }

    /**
     * Builds a deadline from the user's input, splitting it at the {@value #MARKER_BY} marker.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the new deadline, or null if the description or the due time was missing.
     */
    private static Deadline createDeadline(String input) {
        String[] parts = argumentOf(input).split("\\s*" + MARKER_BY + "\\s*", 2);
        String description = parts[0].strip();
        String by = parts.length < 2 ? "" : parts[1].strip();

        if (description.isEmpty() || by.isEmpty()) {
            printProblem("A deadline needs a description and a time, e.g. "
                    + "'deadline return book " + MARKER_BY + " Sunday'.");
            return null;
        }
        return new Deadline(description, by);
    }

    /**
     * Builds an event from the user's input, splitting it at the {@value #MARKER_FROM}
     * and {@value #MARKER_TO} markers.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the new event, or null if the description, start or end was missing.
     */
    private static Event createEvent(String input) {
        String[] fromParts = argumentOf(input).split("\\s*" + MARKER_FROM + "\\s*", 2);
        String description = fromParts[0].strip();
        String from = "";
        String to = "";

        if (fromParts.length == 2) {
            String[] toParts = fromParts[1].split("\\s*" + MARKER_TO + "\\s*", 2);
            from = toParts[0].strip();
            to = toParts.length < 2 ? "" : toParts[1].strip();
        }

        if (description.isEmpty() || from.isEmpty() || to.isEmpty()) {
            printProblem("An event needs a description, a start and an end, e.g. "
                    + "'event project meeting " + MARKER_FROM + " Mon 2pm " + MARKER_TO + " 4pm'.");
            return null;
        }
        return new Event(description, from, to);
    }

    /**
     * Reports that Floppy did not recognise the command word the user typed.
     *
     * @param input the whole line the user entered, already stripped.
     */
    private static void printUnknownCommandResponse(String input) {
        String commandWord = input.split("\\s+")[0];
        printProblem("'" + commandWord + "'? That's not in my directory. I know "
                + "todo, deadline, event, list, mark, unmark and bye.");
    }

    /**
     * Prints every task Floppy is holding, numbered from 1.
     *
     * @param tasks     the storage array; only the first {@code taskCount} slots are filled.
     * @param taskCount how many slots are actually in use.
     */
    private static void printTasks(Task[] tasks, int taskCount) {
        System.out.println(HORIZONTAL_LINE);
        if (taskCount == 0) {
            System.out.println(INDENT + "*spins, finds nothing* Not a single byte in here yet.");
        } else {
            System.out.println(INDENT + "*rattling through the index*");
            System.out.println(INDENT + "Here are the tasks in your list:");
            for (int i = 0; i < taskCount; i++) {
                System.out.println(INDENT + (i + 1) + "." + tasks[i]);
            }
        }
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Reports that a task changed its done status.
     *
     * @param message the sentence describing what happened.
     * @param task    the task whose status changed.
     */
    private static void printStatusChanged(String message, Task task) {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(INDENT + message);
        System.out.println(INDENT_DETAIL + task);
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Reports that Floppy could not carry out a command.
     *
     * @param explanation what went wrong, in Floppy's own words.
     */
    private static void printProblem(String explanation) {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(INDENT + "*stutters* " + explanation);
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Responds to an empty line. Storing a blank task would clutter the list,
     * so Floppy stays in character and ignores it instead.
     */
    private static void printBlankInputResponse() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(INDENT + "*reads an empty sector* ...that was a whole lot of nothing.");
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Responds when storage is full. The brief says to assume this never happens,
     * but saying so beats crashing with an ArrayIndexOutOfBoundsException.
     */
    private static void printDiskFullResponse() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(INDENT + "*grinding noise* Disk full at " + MAX_TASKS + " tasks.");
        System.out.println(INDENT + "I did warn you I was small.");
        System.out.println(HORIZONTAL_LINE);
    }

    /** Prints the farewell message shown when the user exits. */
    private static void printFarewell() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(INDENT + "*spinning down... ejecting*");
        System.out.println(INDENT + "Bye. Hope to see you again soon!");
        System.out.println(INDENT + "Please don't leave me in a drawer for another 20 years.");
        System.out.println(HORIZONTAL_LINE);
    }
}
