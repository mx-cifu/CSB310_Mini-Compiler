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
    private int line;
    private int pos;
    private int position;
    private char chr;
    private String s;
    private TokenType prevToken;

    Map<String, TokenType> keywords = new HashMap<>();

    static class Token {
        public TokenType tokentype;
        public String value;
        public int line;
        public int pos;
        Token(TokenType token, String value, int line, int pos) {
            this.tokentype = token; this.value = value; this.line = line; this.pos = pos;
        }
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

    static enum TokenType {
        End_of_input, Op_multiply,  Op_divide, Op_mod, Op_add, Op_subtract,
        Op_negate, Op_not, Op_less, Op_lessequal, Op_greater, Op_greaterequal,
        Op_equal, Op_notequal, Op_assign, Op_and, Op_or, Keyword_if,
        Keyword_else, Keyword_while, Keyword_print, Keyword_putc, LeftParen, RightParen,
        LeftBrace, RightBrace, Semicolon, Comma, Identifier, Integer, String
    }

    static void error(int line, int pos, String msg) {
        if (line > 0 && pos > 0) {
            System.out.printf("%s in line %d, pos %d\n", msg, line, pos);
        } else {
            System.out.println(msg);
        }
        System.exit(1);
    }

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

    /* Token follow(char expect, TokenType ifyes, TokenType ifno, int line, int pos) {
        if (getNextChar() == expect) {
            getNextChar();
            return new Token(ifyes, "", line, pos);
        }
        if (ifno == TokenType.End_of_input) {
            error(line, pos, String.format("follow: unrecognized character: (%d) '%c'", (int)this.chr, this.chr));
        }
        return new Token(ifno, "", line, pos);
    } */

    Token follow(char expect, TokenType ifyes, TokenType ifno, int line, int pos) {
        if (this.s.charAt(this.position + 1) == expect) {
            getNextChar();
            return new Token(ifyes, "", line, pos);
        } else {
            return new Token(ifno, "", line, pos);
        }
    }
    //So the idea is, you process the char and return its int representation
    //Maybe the Parser is what identifies it as a char, not the Lexer?
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
//        return new Token(TokenType.Integer, "" + n, line, pos);
    }
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
    Token div_or_comment(int line, int pos) { // handle division or comments
        // code here
        //if // aka the next symbol is also /, COMMENT - skip to the next line
        char nextSymbol = this.s.charAt(this.position + 1);
        if (nextSymbol == '/') {
            getNextChar(); //skip it
            while (this.line == line) {
                //if the next char is end of file, return end of input token, otherwise continue
                if (getNextChar() == '\u0000') return new Token(TokenType.End_of_input, "", this.line, this.pos);
            }
            return getToken();
            //if /* aka the next symbol is * COMMENT
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
    Token identifier_or_integer(int line, int pos) { // handle identifiers and integers
        StringBuilder text = new StringBuilder();
        text.append(chr);
        int posi = this.position + 1;
        // code here
        //append the first letter
        //while in valid index range
        //get the following letter
        //while the following letter is a letter or a number, append it
        //if not, return and exit, including if EOF
        while (posi < this.s.length() && Character.isLetterOrDigit(this.s.charAt(posi))) {
            text.append(getNextChar());
            posi++;
        }
        String value = text.toString();

        //if it's a keyword...
        TokenType lookup = keywords.get(value);
        if (lookup != null) {
            return new Token(lookup, "", line, pos);
        }
        //if it's an int...
        if(value.matches("^-?\\d+$")) {
            return new Token(TokenType.Integer, value, line, pos);
        }
        //if not, it must be an identifier
        return new Token(TokenType.Identifier, "\t" + value, line, pos);
    }

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
            //TODO: MINUS VS NEGATE
            case '-':
                //if the previous token was an identifier or integer, then binary
                if (prevToken == TokenType.Identifier || prevToken == TokenType.Integer) {
                    yield new Token(TokenType.Op_subtract, "", line, pos);
                } else {
                    //else, unary
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