
import org.junit.jupiter.api.Test;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ParserTest {


    public static void main(String[] args) throws FileNotFoundException {
        testFileOutputCount();
        testFileOutputHello();
        testFileOutputLoop();
    }

    @Test
    private static void testFileOutputHello() throws FileNotFoundException {

        Scanner correctScanner = new Scanner(new File("src/main/resources/correctHello"));
        Scanner actualScanner = new Scanner(new File("src/main/resources/hello.par"));

        String correctString = null;
        String actualString = null;
        while (correctScanner.hasNext() && actualScanner.hasNext()) {
            correctString = correctString + correctScanner.nextLine();
            actualString = actualString + actualScanner.nextLine();

        }
        assertEquals(correctString, actualString);
    }

    @Test
    private static void testFileOutputCount() throws FileNotFoundException {
        Scanner correctScanner = new Scanner(new File("src/main/resources/correctCount"));
        Scanner actualScanner = new Scanner(new File("src/main/resources/count.par"));

        String correctString = null;
        String actualString = null;
        while (correctScanner.hasNext() && actualScanner.hasNext()) {
            correctString = correctString + correctScanner.nextLine();
            actualString = actualString + actualScanner.nextLine();

        }
        assertEquals(correctString, actualString);

    }

    @Test
    private static void testFileOutputLoop() throws FileNotFoundException {
        Scanner correctScanner = new Scanner(new File("src/main/resources/correctLoop"));
        Scanner actualScanner = new Scanner(new File("src/main/resources/loop.par"));

        String correctString = null;
        String actualString = null;
        while (correctScanner.hasNext() && actualScanner.hasNext()) {
            correctString = correctString + correctScanner.nextLine();
            actualString = actualString + actualScanner.nextLine();

        }
        assertEquals(correctString, actualString);

    }

}
