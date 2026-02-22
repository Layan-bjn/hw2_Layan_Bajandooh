
//LibraryBookTracker.java
import exceptions.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LibraryBookTracker {

    private static int validRecords = 0;
    private static int searchResults = 0;
    private static int booksAdded = 0;
    private static int errorsCount = 0;

    public static void main(String[] args) {
        try {
            if (args.length < 2)
                throw new InsufficientArgumentsException("Expected at least 2 arguments");
            if (!args[0].endsWith(".txt"))
                throw new InvalidFileNameException("File does not end with .txt");

            File catalogFile = new File(args[0]);
            CreateFile(catalogFile);

            ArrayList<Book> bookList = new ArrayList<>();
            Scanner src = new Scanner(catalogFile);
            while (src.hasNextLine()) {
                String line = src.nextLine();
                try {
                    if (line.trim().isEmpty())
                        continue;
                    bookList.add(parseBook(line));
                    validRecords++;
                } catch (Exception e) {
                    errorsCount++;
                    logError(line, e);
                }
            }
            src.close();

            String operation = args[1];
            if (operation.contains(":")) {
                try {
                    Book newBook = parseBook(operation);
                    bookList.add(newBook);
                    sortBooks(bookList);
                    wrightToCatalog(catalogFile, bookList);
                    booksAdded++;
                    System.out.println("Book added successfully:");
                    printHeader();
                    printBookDetails(newBook);
                } catch (Exception e) {
                    errorsCount++;
                    logError(operation, e);
                    System.out.println("Error adding book: " + e.getMessage());
                }
            } else if (operation.length() == 13 && operation.matches("\\d+")) {
                startISBNSearch(bookList, operation);
            } else {
                startTitleSearch(bookList, operation);
            }

        } catch (InsufficientArgumentsException | InvalidFileNameException e) {
            errorsCount++;
            logError(args.length > 1 ? args[1] : "Not Available", e);
            System.out.println(e.getMessage());
        } catch (BookCatalogException e) {
            System.out.println("Catalog Error: " + e.getMessage());
            logError(args[1], e);
        } catch (IOException e) {
            System.out.println("File Error: " + e.getMessage());
        } finally {
            printStatistics();
            System.out.println("Thank you for using the Library Book Tracker.");
        }
    }

    private static void CreateFile(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();
        if (!file.exists())
            file.createNewFile();
    }

    private static Book parseBook(String line) throws MalformedBookEntryException, InvalidISBNException {
        String[] details = line.split(":");
        if (details.length != 4)
            throw new MalformedBookEntryException("Missing or extra fields");

        String title = details[0].trim();
        String author = details[1].trim();
        String isbn = details[2].trim();
        int copies;

        if (title.isEmpty())
            throw new MalformedBookEntryException("Title is empty");
        if (author.isEmpty())
            throw new MalformedBookEntryException("Author is empty");
        if (!isbn.matches("\\d{13}"))
            throw new InvalidISBNException("ISBN must be exactly 13 digits");

        try {
            copies = Integer.parseInt(details[3].trim());
            if (copies <= 0)
                throw new MalformedBookEntryException("Copies must be positive number");
            return new Book(title, author, isbn, copies);
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException("Copies must be a valid number");
        }
    }

    private static void sortBooks(ArrayList<Book> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            for (int j = 0; j < list.size() - i - 1; j++) {
                if (list.get(j).getTitle().compareToIgnoreCase(list.get(j + 1).getTitle()) > 0) {
                    Book temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }
    }

    private static void wrightToCatalog(File file, ArrayList<Book> list) throws IOException {
        FileWriter fw = new FileWriter(file);
        PrintWriter pw = new PrintWriter(fw);
        for (Book b : list) {
            pw.println(b.toString());
        }
        pw.close();
        fw.close();
    }

    private static void startISBNSearch(ArrayList<Book> list, String isbn) throws DuplicateISBNException {
        Book found = null;
        int count = 0;
        for (Book b : list) {
            if (b.getISBN().equals(isbn)) {
                found = b;
                count++;
            }
        }
        if (count > 1)
            throw new DuplicateISBNException("Duplicated book found with " + isbn);
        if (found != null) {
            printHeader();
            printBookDetails(found);
            searchResults++;
        } else {
            System.out.println("No book found with ISBN: " + isbn);
        }
    }

    private static void startTitleSearch(ArrayList<Book> list, String keyword) {
        boolean first = true;
        for (Book b : list) {
            if (b.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                if (first) {
                    printHeader();
                    first = false;
                }
                printBookDetails(b);
                searchResults++;
            }
        }
        if (searchResults == 0)
            System.out.println("No matches found for: " + keyword);
    }

    private static void logError(String text, Exception e) {
        try {
            File LogFile = new File("errors.log");
            FileWriter fw = new FileWriter(LogFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String errorName = e.getClass().getSimpleName();
            String message = e.getMessage();
            bw.write("[" + time + "] INVALID LINE: \"" + text + "\" -" + errorName + ": " + message);
            bw.newLine();
            bw.close();
            fw.close();

        } catch (IOException ex) {
            System.out.println("Could not write to errors.log");
        }
    }

    private static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s%n", "Title", "Author", "ISBN", "Copies");
    }

    private static void printBookDetails(Book b) {
        System.out.printf("%-30.30s %-20.20s %-15s %5d%n", b.getTitle(), b.getAuthor(), b.getISBN(), b.getCopies());
    }

    private static void printStatistics() {
        System.out.println("\n--- Statistics ---");
        System.out.println("Valid records : " + validRecords);
        System.out.println("Search results : " + searchResults);
        System.out.println("Books added: " + booksAdded);
        System.out.println("Errors encountered: " + errorsCount);
        System.out.println("------------------");
    }
}