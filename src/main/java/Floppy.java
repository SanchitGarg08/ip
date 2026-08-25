import java.util.Scanner;

/**
 * Floppy is a command line chatbot with the personality of a 1.44 MB floppy disk:
 * it whirrs, it clicks, and it is delighted to be useful again after decades in a drawer.
 *
 * <p>At this stage (Level-3) Floppy remembers the tasks the user types, lists them on
 * demand, and tracks which ones are done. It exits on {@code bye}.
 */
public class Floppy {

    /** Divider printed above and below every block of Floppy's output. */
    private static final String HORIZONTAL_LINE =
            "    ____________________________________________________________";

    /** Indentation placed in front of every line Floppy speaks. */
    private static final String INDENT = "     ";

    /** ASCII art shown once at startup. */
    private static final String BANNER =
            " _____ _                      \n"
            + "|  ___| | ___  _ __  _ __  _   _\n"
            + "| |_  | |/ _ \\| '_ \\| '_ \\| | | |\n"
            + "|  _| | | (_) | |_) | |_) | |_| |\n"
            + "|_|   |_|\\___/| .__/| .__/ \\__, |\n"
            + "              |_|   |_|    |___/";

    /** The command that makes Floppy exit. */
    private static final String EXIT_COMMAND = "bye";

    /** The command that makes Floppy print every task it is holding. */
    private static final String LIST_COMMAND = "list";

    /** The command that marks a task as done. */
    private static final String MARK_COMMAND = "mark";

    /** The command that marks a task as not done. */
    private static final String UNMARK_COMMAND = "unmark";

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

            if (input.equalsIgnoreCase(EXIT_COMMAND)) {
                break;
            }

            if (input.isEmpty()) {
                printBlankInputResponse();
            } else if (input.equalsIgnoreCase(LIST_COMMAND)) {
                printTasks(tasks, taskCount);
            } else if (isCommand(input, MARK_COMMAND)) {
                changeTaskStatus(tasks, taskCount, input, true);
            } else if (isCommand(input, UNMARK_COMMAND)) {
                changeTaskStatus(tasks, taskCount, input, false);
            } else if (taskCount == MAX_TASKS) {
                printDiskFullResponse();
            } else {
                tasks[taskCount] = new Task(input);
                printTaskAdded(tasks[taskCount], taskCount);
                taskCount++;
            }
        }

        printFarewell();
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
     * Marks a task as done or not done, then reports the outcome to the user.
     * Reports a problem instead if the task number is missing, not a number,
     * or outside the range of stored tasks.
     *
     * @param tasks         the storage array.
     * @param taskCount     how many slots are actually in use.
     * @param input         the whole line the user entered, already stripped.
     * @param shouldBeDone  true to mark the task done, false to mark it not done.
     */
    private static void changeTaskStatus(Task[] tasks, int taskCount, String input, boolean shouldBeDone) {
        String[] parts = input.split("\\s+", 2);

        if (parts.length < 2) {
            printProblem("Which one? Give me a number, e.g. '" + parts[0] + " 2'.");
            return;
        }

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            printProblem("'" + parts[1] + "' is not a number, and I only speak in sectors.");
            return;
        }

        if (taskNumber < 1 || taskNumber > taskCount) {
            printProblem("I have " + taskCount + " task(s). There is nothing at number " + taskNumber + ".");
            return;
        }

        Task task = tasks[taskNumber - 1];
        if (shouldBeDone) {
            task.markAsDone();
            printStatusChanged("*clack* Nice! I've marked this task as done:", task);
        } else {
            task.markAsNotDone();
            printStatusChanged("*rewinds* OK, I've marked this task as not done yet:", task);
        }
    }

    /** Prints the banner and welcome message shown when Floppy starts up. */
    private static void printGreeting() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(BANNER);
        System.out.println(INDENT + "*click... whirr... clunk*");
        System.out.println(INDENT + "Hello! I'm Floppy, 1.44 MB of pure determination.");
        System.out.println(INDENT + "Tell me a task and I'll hold onto it.");
        System.out.println(INDENT + "Try 'list', 'mark 1', 'unmark 1', or 'bye'.");
        System.out.println(INDENT + "What can I do for you?");
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Confirms that a task has been stored.
     *
     * @param task      the task that was just stored.
     * @param taskIndex position the task was stored at, used to pick the drive noise.
     */
    private static void printTaskAdded(Task task, int taskIndex) {
        String noise = DRIVE_NOISES[taskIndex % DRIVE_NOISES.length];
        System.out.println(HORIZONTAL_LINE);
        System.out.println(INDENT + noise + " added: " + task);
        System.out.println(HORIZONTAL_LINE);
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
        System.out.println(INDENT + "  " + task);
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
