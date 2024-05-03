import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Rory Hackney
 * Unit tests for Lexer
 */
public class LexerTests {
    /**
     * Tests comments of the form //comment, /*comment*{@literal /}
     */
    @Test
    void commentTest() {
        //should return token String with value str
        //test //comment
        Lexer lexer = new Lexer("//comment\n\"str\"");
        Lexer.Token x = lexer.getToken();
        assertEquals(Lexer.TokenType.String, x.tokentype);
        assertEquals("str", x.value);

        //test /*comment*/
        lexer = new Lexer("/*comment*/\"str\"");
        x = lexer.getToken();
        assertEquals(Lexer.TokenType.String, x.tokentype);
        assertEquals("str", x.value);

        /*comm
        ent*/
        lexer = new Lexer("/*comm\nent*/\"str\"");
        x = lexer.getToken();
        assertEquals(Lexer.TokenType.String, x.tokentype);
        assertEquals("str", x.value);

        //should return END_OF_FILE token if nothing after
        //comment
        lexer = new Lexer("//comment");
        assertEquals(Lexer.TokenType.End_of_input, lexer.getToken().tokentype);

        /*comment*/
        lexer = new Lexer("/*comment*/");
        assertEquals(Lexer.TokenType.End_of_input, lexer.getToken().tokentype);

        /*comm
        ent*/
        lexer = new Lexer("/*comm\nent*/");
        assertEquals(Lexer.TokenType.End_of_input, lexer.getToken().tokentype);
    }

    @Test
    void stringTest() {
        Lexer lexer = new Lexer("\"bees\"");
        Lexer.Token x = lexer.getToken();
        assertEquals(Lexer.TokenType.String, x.tokentype);
        assertEquals("bees", x.value);
    }

    @Test
    void charTest() {
        Lexer lexer = new Lexer("'a'");
        Lexer.Token x = lexer.getToken();
        assertEquals(Lexer.TokenType.Integer, x.tokentype);
        assertEquals(""+(int)'a', x.value);
    }
}
