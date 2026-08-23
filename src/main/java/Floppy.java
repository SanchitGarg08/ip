import java.util.Scanner;

/**
 * Floppy is a command line chatbot with the personality of a 1.44 MB floppy disk:
 * it whirrs, it clicks, and it is delighted to be useful again after decades in a drawer.
 *
 * <p>At this stage (Level-1) Floppy simply echoes back whatever the user types,
 * and exits when the user types {@code bye}.
 */
public class Floppy {

    /** Divider printed above and below every block of Floppy's output. */
    private static final String HORIZONTAL_LINE =
            "    ____________________________________________________________";

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

    /**
     * Drive noises used to introduce each echoed command. Floppy cycles through them
     * so that repeated commands do not produce identical replies, which makes the
     * chatbot feel more alive than a single fixed prefix would.
     */
    private static final String[] DRIVE_NOISES = {
        "*whirr-click*",
        "*chk-chk-chk*",
        "*seeking track 00*",
        "*clunk... spinning up*"
    };

    public static void main(String[] args) {
        printGreeting();

        Scanner in = new Scanner(System.in);
        int commandCount = 0;

        // hasNextLine() guards against the input stream ending without a "bye",
        // which happens when input is piped in from a file rather than typed.
        while (in.hasNextLine()) {
            String input = in.nextLine();

            if (input.equalsIgnoreCase(EXIT_COMMAND)) {
                break;
            }

            if (input.isBlank()) {
                printBlankInputResponse();
                continue;
            }

            printEcho(input, commandCount);
            commandCount++;
        }

        printFarewell();
    }

    /** Prints the banner and welcome message shown when Floppy starts up. */
    private static void printGreeting() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(BANNER);
        System.out.println("     *click... whirr... clunk*");
        System.out.println("     Hello! I'm Floppy, 1.44 MB of pure determination.");
        System.out.println("     Careful, I bruise easily and I fear magnets.");
        System.out.println("     What can I do for you?");
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Echoes one command back to the user, prefixed with a drive noise.
     *
     * @param input        the exact text the user entered
     * @param commandCount how many commands have been echoed so far, used to pick the noise
     */
    private static void printEcho(String input, int commandCount) {
        String noise = DRIVE_NOISES[commandCount % DRIVE_NOISES.length];
        System.out.println(HORIZONTAL_LINE);
        System.out.println("     " + noise + " I read back: " + input);
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Responds to an empty line. Echoing nothing back would look like a bug,
     * so Floppy stays in character instead.
     */
    private static void printBlankInputResponse() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println("     *reads an empty sector* ...that was a whole lot of nothing.");
        System.out.println(HORIZONTAL_LINE);
    }

    /** Prints the farewell message shown when the user exits. */
    private static void printFarewell() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println("     *spinning down... ejecting*");
        System.out.println("     Bye. Hope to see you again soon!");
        System.out.println("     Please don't leave me in a drawer for another 20 years.");
        System.out.println(HORIZONTAL_LINE);
    }
}
