package floppy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import floppy.task.Deadline;
import floppy.task.Event;
import floppy.task.Task;
import floppy.task.Todo;

/**
 * Keeps Floppy's tasks on the hard disk so that they survive between runs.
 * Each task is written as one line of text in the data file.
 */
public class Storage {

    /**
     * Matches the separator between fields on a line of the data file. Spaces around the
     * bar are optional, so a line that was edited by hand still reads back correctly.
     */
    private static final String FIELD_SEPARATOR_REGEX = "\\s*\\|\\s*";

    /** Where the tasks are kept, relative to the folder Floppy is run from. */
    private final Path filePath;

    /**
     * Constructs a storage that keeps tasks in the given file.
     *
     * @param filePath where to keep the tasks.
     */
    public Storage(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Returns the tasks kept in the data file, in the order they were saved.
     *
     * @return the saved tasks.
     * @throws FloppyException if the file cannot be read or holds a task Floppy does not recognise.
     */
    public List<Task> load() throws FloppyException {
        List<String> lines;
        try {
            lines = Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new FloppyException("I couldn't read " + filePath + ": " + e.getMessage());
        }

        List<Task> tasks = new ArrayList<>();
        for (String line : lines) {
            tasks.add(decode(line));
        }
        return tasks;
    }

    /**
     * Returns the task described by one line of the data file.
     *
     * @param line a line written by {@link Task#toFileFormat()}.
     * @return the task the line describes, marked done if it was saved as done.
     * @throws FloppyException if the line starts with a type code Floppy does not recognise.
     */
    private static Task decode(String line) throws FloppyException {
        String[] fields = line.split(FIELD_SEPARATOR_REGEX);
        String typeCode = fields[0];
        boolean isDone = fields[1].equals(Task.FILE_DONE);
        String description = fields[2];

        Task task = switch (typeCode) {
        case Todo.FILE_TYPE_CODE -> new Todo(description);
        case Deadline.FILE_TYPE_CODE -> new Deadline(description, fields[3]);
        case Event.FILE_TYPE_CODE -> new Event(description, fields[3], fields[4]);
        default -> throw new FloppyException("Unknown task type '" + typeCode + "' in the data file.");
        };

        if (isDone) {
            task.markAsDone();
        }
        return task;
    }

    /**
     * Writes every task to the data file, replacing whatever the file held before.
     *
     * @param tasks the tasks to keep.
     * @throws FloppyException if the file cannot be written.
     */
    public void save(List<Task> tasks) throws FloppyException {
        List<String> lines = new ArrayList<>();
        for (Task task : tasks) {
            lines.add(task.toFileFormat());
        }

        try {
            // A fresh copy of Floppy has no data folder yet, so create it before writing.
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, lines);
        } catch (IOException e) {
            throw new FloppyException("I couldn't write to " + filePath + ": " + e.getMessage());
        }
    }
}
