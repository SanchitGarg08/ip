package floppy;

import java.nio.file.Path;
import java.util.List;

import floppy.task.Task;

/**
 * Floppy is a command line chatbot with the personality of a 1.44 MB floppy disk:
 * it whirrs, it clicks, and it is delighted to be useful again after decades in a drawer.
 *
 * <p>Floppy tracks three kinds of task -- todos, deadlines and events -- lists them on
 * demand, records which ones are done, deletes them when asked, and saves them to disk
 * so they survive a restart. It exits on {@code bye}.
 *
 * <p>This class holds the pieces together and decides what each command does. The work
 * itself belongs to {@link Ui}, {@link Storage}, {@link Parser} and {@link TaskList}.
 */
public class Floppy {

    /**
     * Where Floppy keeps its tasks between runs. The path is relative, so it resolves
     * against the folder Floppy is run from, and it is built from separate parts so
     * that the right folder separator is used on every operating system.
     */
    private static final Path DATA_FILE = Path.of("data", "floppy.txt");

    /** Loads and saves the tasks. */
    private final Storage storage;

    /** Reads the user's commands and prints Floppy's replies. */
    private final Ui ui;

    /** The tasks Floppy is holding. Filled in by {@link #run()} when the saved tasks load. */
    private TaskList tasks;

    /**
     * Constructs a chatbot that keeps its tasks in the given file.
     *
     * @param filePath where the tasks are saved between runs.
     */
    public Floppy(Path filePath) {
        this.ui = new Ui();
        this.storage = new Storage(filePath);
    }

    /**
     * Starts the chatbot and runs it until the user says {@code bye} or the input runs out.
     */
    public void run() {
        ui.showGreeting();
        tasks = loadTasks();

        // hasNextCommand() is false once the input runs out, which happens when
        // input is piped in from a file rather than typed.
        while (ui.hasNextCommand()) {
            String input = ui.readCommand();

            if (input.equalsIgnoreCase(Parser.COMMAND_EXIT)) {
                break;
            }

            try {
                handleCommand(input);
                storage.save(tasks.asList());
            } catch (FloppyException e) {
                ui.showProblem(e.getMessage());
            }
        }

        ui.showFarewell();
    }

    /**
     * Runs Floppy.
     *
     * @param args command line arguments, which Floppy does not use.
     */
    public static void main(String[] args) {
        new Floppy(DATA_FILE).run();
    }

    /**
     * Loads the saved tasks. Warns the user about any unreadable lines that were skipped,
     * and starts with no tasks if the data file cannot be read at all.
     *
     * @return the tasks to start with.
     */
    private TaskList loadTasks() {
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
     * @param input the whole line the user entered, already stripped.
     * @throws FloppyException if the command cannot be carried out as typed.
     */
    private void handleCommand(String input) throws FloppyException {
        if (input.isEmpty()) {
            ui.showBlankInput();
        } else if (Parser.isCommand(input, Parser.COMMAND_LIST)) {
            listTasks(input);
        } else if (Parser.isCommand(input, Parser.COMMAND_MARK)) {
            changeTaskStatus(input, true);
        } else if (Parser.isCommand(input, Parser.COMMAND_UNMARK)) {
            changeTaskStatus(input, false);
        } else if (Parser.isCommand(input, Parser.COMMAND_DELETE)) {
            deleteTask(input);
        } else if (Parser.isCommand(input, Parser.COMMAND_TODO)) {
            addTask(Parser.parseTodo(input));
        } else if (Parser.isCommand(input, Parser.COMMAND_DEADLINE)) {
            addTask(Parser.parseDeadline(input));
        } else if (Parser.isCommand(input, Parser.COMMAND_EVENT)) {
            addTask(Parser.parseEvent(input));
        } else {
            throw new FloppyException("'" + Parser.commandWordOf(input) + "'? That's not in my directory. "
                    + "I know todo, deadline, event, list, mark, unmark, delete and bye.");
        }
    }

    /**
     * Shows every task Floppy is holding, or reports a problem if the list command
     * was given something after it.
     *
     * @param input the whole line the user entered, already stripped.
     * @throws FloppyException if the list command was given something after it.
     */
    private void listTasks(String input) throws FloppyException {
        String argument = Parser.argumentOf(input);

        if (!argument.isEmpty()) {
            throw new FloppyException("'" + Parser.COMMAND_LIST + "' takes nothing after it. "
                    + "Drop the '" + argument + "' and I'll read the whole disk.");
        }

        ui.showTasks(tasks.asList());
    }

    /**
     * Marks the task the user picked as done or not done, then reports the outcome.
     *
     * @param input        the whole line the user entered, already stripped.
     * @param shouldBeDone true to mark the task done, false to mark it not done.
     * @throws FloppyException if the command does not name a task that exists.
     */
    private void changeTaskStatus(String input, boolean shouldBeDone) throws FloppyException {
        Task task = findTask(input);

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
     * @param input the whole line the user entered, already stripped.
     * @throws FloppyException if the command does not name a task that exists.
     */
    private void deleteTask(String input) throws FloppyException {
        Task task = findTask(input);
        tasks.remove(task);
        ui.showTaskDeleted(task, tasks.size());
    }

    /**
     * Stores a task and reports it to the user.
     *
     * @param task the task to store.
     */
    private void addTask(Task task) {
        tasks.add(task);
        ui.showTaskAdded(task, tasks.size());
    }

    /**
     * Returns the task named by the number in the user's command.
     *
     * @param input the whole line the user entered, already stripped.
     * @return the task the user picked.
     * @throws FloppyException if the number is missing, not a number, or names no stored task.
     */
    private Task findTask(String input) throws FloppyException {
        int taskNumber = Parser.parseTaskNumber(input);

        if (tasks.isEmpty()) {
            throw new FloppyException("There's nothing on me to "
                    + Parser.commandWordOf(input).toLowerCase()
                    + " yet. Add a task first, e.g. 'todo borrow book'.");
        }

        if (taskNumber < 1 || taskNumber > tasks.size()) {
            throw new FloppyException("I have " + Ui.describeCount(tasks.size())
                    + ". There is nothing at number " + Parser.argumentOf(input) + ".");
        }

        return tasks.get(taskNumber - 1);
    }
}
