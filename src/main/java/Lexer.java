//Rory Hackney

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Represents the lexer portion of the compiler, which takes in a text file / string and returns its tokens
 */
public class Lexer {
    /** Line number of the current char for display */
    private int line;
    /** Position within the line of the current char for display */
    private int pos;
    /** Absolute position of the pointer to the current char being evaluated in s*/
    private int position;
    /** Value of the current character being evaluated */
    private char chr;
    /** String representation of the file being processed, as a single String containing \n to separate lines in the file*/
    private String s;
    /** The type of the token before the current character */
    private TokenType prevToken;

    /**
     * Special keywords in the language, including if, else, while, print, putc
     */
    Map<String, TokenType> keywords = new HashMap<>();

    /**
     * Represents a lexeme with a particular token type, value, line and position (location) in the source file
     */
    static class Token {
        /** Category of Token, one of TokenType enum, including operators, keywords, symbols, identifiers, and literals*/
        public TokenType tokentype;
        /** Value of the Token object, which may be empty for some token types*/
        public String value;
        /** Line of the file this Token is on*/
        public int line;
        /** Position within the line this Token starts on*/
        public int pos;

        /**
         * Constructor for Token objects
         * @param token category of token, one of TokenType enum
         * @param value the value of the Token, which may be empty for some token types
         * @param line the line of the file this Token is on
         * @param pos the position within the line this Token starts on
         */
        Token(TokenType token, String value, int line, int pos) {
            this.tokentype = token; this.value = value; this.line = line; this.pos = pos;
        }

        /**
         * Returns a String representation of this Token
         * @return String representation of this Token
         */
        @Override
        public String toString() {
            String result = String.format("%5d  %5d %-15s", this.line, this.pos, this.tokentype);
            switch (this.tokentype) {
                case Integer:
                    result += String.format("  %4s", value);
                    break;
                case Identifier:
                    result += String.format(" %s", value);
                    break;
                case String:
                    result += String.format(" \"%s\"", value);
                    break;
            }
            return result;
        }
    }

    /**
     * Represents all possible token types
     */
    static enum TokenType {
        End_of_input, Op_multiply,  Op_divide, Op_mod, Op_add, Op_subtract,
        Op_negate, Op_not, Op_less, Op_lessequal, Op_greater, Op_greaterequal,
        Op_equal, Op_notequal, Op_assign, Op_and, Op_or, Keyword_if,
        Keyword_else, Keyword_while, Keyword_print, Keyword_putc, LeftParen, RightParen,
        LeftBrace, RightBrace, Semicolon, Comma, Identifier, Integer, String
    }

    /**
     * Displays an error message with bug location and exits program
     * @param line the line number causing the error
     * @param pos the position within the line causing the error
     * @param msg a message explaining the error
     */
    static void error(int line, int pos, String msg) {
        if (line > 0 && pos > 0) {
            System.out.printf("%s in line %d, pos %d\n", msg, line, pos);
        } else {
            System.out.println(msg);
        }
        System.exit(1);
    }

    /**
     * Constructor of Lexer object which collects all the Tokens in the source String
     * @param source String to perform lexer operation on
     */
    Lexer(String source) {
        this.line = 1;
        this.pos = -1;
        this.position = -1;
        this.s = source;
        this.chr = this.s.charAt(0);
        this.keywords.put("if", TokenType.Keyword_if);
        this.keywords.put("else", TokenType.Keyword_else);
        this.keywords.put("print", TokenType.Keyword_print);
        this.keywords.put("putc", TokenType.Keyword_putc);
        this.keywords.put("while", TokenType.Keyword_while);
    }

    /**
     * Checks if the following char matches expected char, and returns the appropriate Token in each case
     * @param expect expected next char
     * @param ifyes Token to return if next char matches expect
     * @param ifno Token to return if next char doesn't match expect
     * @param line current line number
     * @param pos current position in the line
     * @return appropriate Token based on expected char
     */
    Token follow(char expect, TokenType ifyes, TokenType ifno, int line, int pos) {
        if (this.s.charAt(this.position + 1) == expect) {
            getNextChar();
            return new Token(ifyes, "", line, pos);
        }
        if (ifno == TokenType.End_of_input) {
            error(line, pos, String.format("follow: unrecognized character: (%d) '%c'", (int)this.chr, this.chr));
        }
        return new Token(ifno, "", line, pos);

    }
    //So the idea is, you process the char and return its int representation
    //Maybe the Parser is what identifies it as a char, not the Lexer?

    /**
     * Processes a char literal, returning Integer Token with ASCII value or throwing error for malformed chars
     * @param line line the char literal is on
     * @param pos position within the line the char literal starts on
     * @return Integer Token with ASCII value
     */
    Token char_lit(int line, int pos) { // handle character literals
        char c = getNextChar(); // skip opening quote - this is the char
        if(c == '\'') {
            error(line, pos, "Empty char literal");
        }
        // a char_lit should be either '?' or '\n' or '\\'
        char d = getNextChar();
        Token ret = null;
        //if d = ', then c
        if (d == '\'') {
            ret = new Token(TokenType.Integer, "" + (int)c, line, pos);
        }
        //else if c = \
        else if (c == '\\') {
            //if d = n, \n
            if (d == 'n') ret = new Token(TokenType.Integer, "" + (int)'\n', line, pos);
            //if d = \, \\
            else if (d == '\\') ret = new Token(TokenType.Integer, "" + (int)('\\'), line, pos);
            else error(line, pos, "invalid escape character");
            //skip past closing quote, if it wasn't a quote, invalid char literal
            if (getNextChar() != '\'') error(line, pos, "invalid char literal - no closing quote");
        }
        //if neither in form '?' or '\n' or '\\', invalid char lit
        else error(line, pos, "invalid char literal");
        return ret;
    }

    /**
     * Processes a String literal until closing " and returns a String Token or error if not closed
     * @param start char at the start of the String literal, should be "
     * @param line line number the String is on
     * @param pos position within the line the String starts on
     * @return String Token containing the value of the String
     */
    Token string_lit(char start, int line, int pos) { // handle string literals
        StringBuilder result = new StringBuilder();
        // code here
        start = getNextChar();
        while (start != '"') {
            if (start == '\u0000') {
                error(this.line, this.pos, "Reached end of file without closing String");
                return new Token(TokenType.End_of_input, result.toString(), line, pos);
            }
            result.append(start);
            start = getNextChar();
        }
        return new Token(TokenType.String, result.toString(), line, pos);
    }

    /**
     * Processes a token beginning with / and either skips comment or returns Division Operator Token
     * @param line line number the token is on
     * @param pos position within the line the token is on
     * @return Division Operator Token, or if a comment, skips past and returns the following Token instead
     */
    Token div_or_comment(int line, int pos) { // handle division or comments
        //if // aka the next symbol is also /, COMMENT - skip to the next line
        char nextSymbol = this.s.charAt(this.position + 1);
        if (nextSymbol == '/') {
            getNextChar(); //skip it
            while (this.line == line) {
                //if the next char is end of file, return end of input token, otherwise continue
                if (getNextChar() == '\u0000') return new Token(TokenType.End_of_input, "", this.line, this.pos);
            }
            return getToken();
            //if /* aka the next symbol is * COMMENT - skip past comments
        } else if (nextSymbol == '*') {
            getNextChar(); // skip it
            //while the next symbol is not *, go to the next character. If the next char is not /, go past and keep going
            boolean done = false;
            while (! done) {
                nextSymbol = getNextChar();
                if (nextSymbol == '*' && this.s.charAt(this.position + 1) == '/') {
                    getNextChar();
                    done = true;
                }
            }
            return getToken();
        }
        //if not comment, must be division operator
        return new Token(TokenType.Op_divide, null, line, pos);
    }

    /**
     * Processes token and returns Identifier, Integer, or Keyword Token as appropriate
     * @param line line number this Token is on
     * @param pos position within the line this Token starts on
     * @return Identifier, Integer, or Keyword Token as appropriate
     */
    Token identifier_or_integer(int line, int pos) { // handle identifiers and integers
        StringBuilder text = new StringBuilder();
        text.append(chr);
        int posi = this.position + 1;
        //while in valid index range and the following letter is a letter or a number, append it
        while (posi < this.s.length() && Character.isLetterOrDigit(this.s.charAt(posi))) {
            text.append(getNextChar());
            posi++;
        }
        //if not, done building the token
        String value = text.toString();

        //if it's a keyword, return that keyword as a token
        TokenType lookup = keywords.get(value);
        if (lookup != null) {
            return new Token(lookup, "", line, pos);
        }
        //if it's an integer, return Integer Token
        if(value.matches("^-?\\d+$")) {
            return new Token(TokenType.Integer, value, line, pos);
        }

        //if neither an integer nor a keyword, return an Identifier Token
        return new Token(TokenType.Identifier, "\t" + value, line, pos);
    }

    /**
     * Returns the next Token, based on the following char in this Lexers s String
     * @return the next Token
     */
    Token getToken() {
        int line, pos;
        getNextChar();
        while (Character.isWhitespace(this.chr)) {
            getNextChar();
        }
        line = this.line;
        pos = this.pos;

        // switch statement on character for all forms of tokens with return to follow.... one example left for you
        Token result = switch (this.chr) {
            case '\u0000':
                yield new Token(TokenType.End_of_input, "", this.line, this.pos);
            // remaining case statements
            case '\'': yield char_lit(line, pos);
            case '"': yield string_lit('"', this.line, this.pos);
            case '/': yield div_or_comment(line, pos);
            case '(': yield new Token(TokenType.LeftParen, "", line, pos);
            case ')': yield new Token(TokenType.RightParen, "", line, pos);
            case '{': yield new Token(TokenType.LeftBrace, "", line, pos);
            case '}': yield new Token(TokenType.RightBrace, "", line, pos);
            case ';': yield new Token(TokenType.Semicolon, "", line, pos);
            case ',': yield new Token(TokenType.Comma, "", line, pos);
            case '*': yield new Token(TokenType.Op_multiply, "", line, pos);
            case '%': yield new Token(TokenType.Op_mod, "", line, pos);
            case '+': yield new Token(TokenType.Op_add, "", line, pos);
            case '-':
                //if the previous token was an identifier or integer, then binary minus
                if (prevToken == TokenType.Identifier || prevToken == TokenType.Integer) {
                    yield new Token(TokenType.Op_subtract, "", line, pos);
                } else {
                    //else, unary minus
                    yield new Token(TokenType.Op_negate, "", line, pos);
                }
            case '<': // < vs <=
                yield follow('=', TokenType.Op_lessequal, TokenType.Op_less, line, pos);
            case '>': // > vs >=
                yield follow('=', TokenType.Op_greaterequal, TokenType.Op_greater, line, pos);
            case '=': // = vs ==
                yield follow('=', TokenType.Op_equal, TokenType.Op_assign, line, pos);
            case '!':
                yield follow('=', TokenType.Op_notequal, TokenType.Op_not, line, pos);
            case '&':
                getNextChar(); //skip second &
                yield new Token(TokenType.Op_and, "", line, pos);
            case '|':
                getNextChar(); //skip second |
                yield new Token(TokenType.Op_or, "", line, pos);
            default: yield identifier_or_integer(line, pos);
        };
        prevToken = result.tokentype;
        return result;
    }

    /**
     * Returns the next char in this Lexers s String, skipping whitespace
     * @return the next char, skipping whitespace
     */
    char getNextChar() {
        this.pos++;
        this.position++;
        if (this.position >= this.s.length()) {
            this.chr = '\u0000';
            return this.chr;
        }
        this.chr = this.s.charAt(this.position);
        if (this.chr == '\n') {
            this.line++;
            this.pos = 0;
        }
        return this.chr;
    }

    /**
     * Returns a String representation of all the Tokens in this Lexers s String
     * @return String text of all Tokens read from this Lexers s String
     */
    String printTokens() {
        Token t;
        StringBuilder sb = new StringBuilder();
        while ((t = getToken()).tokentype != TokenType.End_of_input) {
            sb.append(t);
            sb.append("\n");
            System.out.println(t);
        }
        sb.append(t);
        System.out.println(t);
        return sb.toString();
    }

    /**
     * Outputs given text to filename.lex
     * @param result String text to output to the file
     * @param filename name of the file to write to, replaces extension with .lex
     */
    static void outputToFile(String result, String filename) {
        try {
            //remove old extension and replace with .lex
            int trimHere = filename.indexOf('.');
            if (trimHere != -1) {
                filename = filename.substring(0, trimHere);
            }
            filename = filename + ".lex";

            //write to file
            FileWriter myWriter = new FileWriter("src/main/resources/" + filename);
            myWriter.write(result);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes tokens in all filenames given in args, and outputs result of each to its own file.lex
     * @param args Command line arguments, should be a list of file names to read from
     */
    public static void main(String[] args) {
        if (1==1) {
            File[] files = new File[args.length];
            Scanner s;
            StringBuilder source;
            Lexer l;
            String result;
            try {
                for (int fileNum = 0; fileNum < args.length; fileNum++) {
                    files[fileNum] = new File("src/main/resources/" + args[fileNum]);
                    s = new Scanner(files[fileNum]);
                    source = new StringBuilder();
                    while (s.hasNext()) {
                        source.append(s.nextLine()).append("\n");
                    }
                    s.close();

                    l = new Lexer(source.toString());
                    result = l.printTokens();
                    outputToFile(result, args[fileNum]);
                }
            } catch(FileNotFoundException e) {
                 error(-1, -1, "Exception: " + e.getMessage());
            }
        } else {
            error(-1, -1, "No args");
        }
    }
}