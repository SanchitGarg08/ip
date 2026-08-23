import java.util.Scanner;

/**
 * Floppy is a command line chatbot with the personality of a 1.44 MB floppy disk:
 * it whirrs, it clicks, and it is delighted to be useful again after decades in a drawer.
 *
 * <p>At this stage (Level-2) Floppy stores whatever the user types and plays the
 * whole collection back when the user types {@code list}. It exits on {@code bye}.
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

    /** The command that makes Floppy print everything it has stored. */
    private static final String LIST_COMMAND = "list";

    /**
     * Largest number of items Floppy can hold. The project brief allows us to assume
     * the user never exceeds this, so a fixed-size array is enough for now.
     */
    private static final int MAX_ITEMS = 100;

    /**
     * Drive noises used to introduce each stored item. Floppy cycles through them
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
        String[] items = new String[MAX_ITEMS];
        int itemCount = 0;

        // hasNextLine() guards against the input stream ending without a "bye",
        // which happens when input is piped in from a file rather than typed.
        while (in.hasNextLine()) {
            String input = in.nextLine();

            if (input.equalsIgnoreCase(EXIT_COMMAND)) {
                break;
            }

            if (input.equalsIgnoreCase(LIST_COMMAND)) {
                printItems(items, itemCount);
            } else if (input.isBlank()) {
                printBlankInputResponse();
            } else if (itemCount == MAX_ITEMS) {
                printDiskFullResponse();
            } else {
                items[itemCount] = input;
                printItemAdded(input, itemCount);
                itemCount++;
            }
        }

        printFarewell();
    }

    /** Prints the banner and welcome message shown when Floppy starts up. */
    private static void printGreeting() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println(BANNER);
        System.out.println("     *click... whirr... clunk*");
        System.out.println("     Hello! I'm Floppy, 1.44 MB of pure determination.");
        System.out.println("     Tell me anything and I'll hold onto it. Type 'list' to hear it back.");
        System.out.println("     What can I do for you?");
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Confirms that an item has been stored.
     *
     * @param item      the exact text the user entered
     * @param itemIndex position the item was stored at, used to pick the drive noise
     */
    private static void printItemAdded(String item, int itemIndex) {
        String noise = DRIVE_NOISES[itemIndex % DRIVE_NOISES.length];
        System.out.println(HORIZONTAL_LINE);
        System.out.println("     " + noise + " added: " + item);
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Prints everything Floppy is holding, numbered from 1.
     *
     * @param items     the storage array; only the first {@code itemCount} slots are filled
     * @param itemCount how many slots are actually in use
     */
    private static void printItems(String[] items, int itemCount) {
        System.out.println(HORIZONTAL_LINE);
        if (itemCount == 0) {
            System.out.println("     *spins, finds nothing* Not a single byte in here yet.");
        } else {
            System.out.println("     *rattling through the index*");
            for (int i = 0; i < itemCount; i++) {
                System.out.println("     " + (i + 1) + ". " + items[i]);
            }
        }
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Responds to an empty line. Storing a blank item would clutter the list,
     * so Floppy stays in character and ignores it instead.
     */
    private static void printBlankInputResponse() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println("     *reads an empty sector* ...that was a whole lot of nothing.");
        System.out.println(HORIZONTAL_LINE);
    }

    /**
     * Responds when storage is full. The brief says to assume this never happens,
     * but saying so beats crashing with an ArrayIndexOutOfBoundsException.
     */
    private static void printDiskFullResponse() {
        System.out.println(HORIZONTAL_LINE);
        System.out.println("     *grinding noise* Disk full at " + MAX_ITEMS + " items.");
        System.out.println("     I did warn you I was small.");
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
