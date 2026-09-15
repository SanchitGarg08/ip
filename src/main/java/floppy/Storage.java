package floppy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import floppy.task.Task;

/**
 * Keeps Floppy's tasks on the hard disk so that they survive between runs.
 * Each task is written as one line of text in the data file.
 */
public class Storage {

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
            throw new FloppyException("*disk error* I couldn't write to " + filePath + ": " + e.getMessage());
        }
    }
}
