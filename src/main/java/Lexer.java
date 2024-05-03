//Rory Hackney

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Lexer {
    private int line;
    private int pos;
    private int position;
    private char chr;
    private String s;

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
//        System.exit(1);
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
    Token follow(char expect, TokenType ifyes, TokenType ifno, int line, int pos) {
        if (getNextChar() == expect) {
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
    //Because we're just supposed to return the int ...
    //I don't know.
    //TODO: remove letter from value return and tidy up comments
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
            if (d == 'n') ret = new Token(TokenType.Integer, "" + (int)'\n' + " '\\n'", line, pos);
            //if d = \, \\
            else if (d == '\\') ret = new Token(TokenType.Integer, "" + (int)('\\') + " '\\\\'", line, pos);
            else error(line, pos, "invalid escape character");
            //skip past closing quote
            //if it wasn't a quote, invalid char literal, error()
            if (getNextChar() != '\'') error(line, pos, "invalid char literal - no closing quote");
        }
        //if neither in form '?' or '\n' or '\\', invalid char lit
        else error(line, pos, "invalid char literal");
        System.out.println('\n');
        return ret;

//        return new Token(TokenType.Integer, "" + n, line, pos);
    }
    Token string_lit(char start, int line, int pos) { // handle string literals
        String result = "";
        // code here
        start = getNextChar();
        while (start != '"') {
            if (start == '\u0000') {
                error(this.line, this.pos, "Reached end of file without closing String");
                return new Token(TokenType.End_of_input, result, line, pos);
            }
            result += start;
            start = getNextChar();
        }
        return new Token(TokenType.String, result, line, pos);
    }
    Token div_or_comment(int line, int pos) { // handle division or comments
        // code here
        // / is division, // or /* */ is comments
        //if / by itself aka the next symbol is not * or /, DIVISION OPERATOR
        //if // aka the next symbol is also /, COMMENT - skip to the next line
        char nextSymbol = getNextChar();
        if (nextSymbol == '/') {
            nextSymbol = getNextChar();
            while (this.line == line) {
                if (getNextChar() == '\u0000') return new Token(TokenType.End_of_input, "", this.line, this.pos);
            }
            //AND the next char is not end of file
            return getToken();
            //if /* aka the next symbol is *, while the next symbol is not *, go to the next character. If the next char is not /, go past and keep going
        } else if (nextSymbol == '*') {
            boolean done = false;
            while (! done) {
                nextSymbol = getNextChar();
                if (nextSymbol == '*' && getNextChar() == '/') {
                    return getToken();
                };
            }
//
//            pos++;
//            //find the next */
//            while (! done) {
//                nextSymbol = this.s.charAt(pos + 1);
//                char followingSymbol = this.s.charAt(pos + 2);
//                pos += 2;
//                if (nextSymbol == '*' && followingSymbol == '/') {
//                    this.pos = pos;
//                    this.position = pos;
//                    return getToken();
//                }
//            }
        }
        return new Token(TokenType.Op_divide, null, line, pos);


//        return getToken();
    }
    Token identifier_or_integer(int line, int pos) { // handle identifiers and integers
        boolean is_number = true;
        StringBuilder text = new StringBuilder(this.s.charAt(pos));
        // code here
        while (Character.isLetterOrDigit(this.s.charAt(pos + 1))) {
            text.append(getNextChar());
            pos++;
        }
        return new Token(TokenType.Identifier, text.toString(), line, pos);
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

        switch (this.chr) {
            case '\u0000': return new Token(TokenType.End_of_input, "", this.line, this.pos);
            case '\'': return char_lit(line, pos);
            case '"': return string_lit('"', this.line, this.pos);
            case '/': return div_or_comment(line, pos);
            // remaining case statements

            default: return identifier_or_integer(line, pos);
        }
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

    static void outputToFile(String result) {
        try {
            FileWriter myWriter = new FileWriter("src/main/resources/hello.lex");
            myWriter.write(result);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (1==1) {
            try {

                File f = new File("src/main/resources/testing1.c");
                Scanner s = new Scanner(f);
                String source = " ";
                String result = " ";
                while (s.hasNext()) {
                    source += s.nextLine() + "\n";
                }
                Lexer l = new Lexer(source);
                result = l.printTokens();

                outputToFile(result);

            } catch(FileNotFoundException e) {
                 error(-1, -1, "Exception: " + e.getMessage());
            }
        } else {
            error(-1, -1, "No args");
        }
    }
}