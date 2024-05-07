//import jdk.incubator.foreign.CLinker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Parser class that takes input from Lexer and interprets it via the grammatical rules
 */
class Parser {

    /**
     * List of tokens from input file
     */
    private List<Token> source;
    /**
     * A single token from within the list, current one
     */
    private Token token;
    /**
     * Current position within the List of Tokens
     */
    private int position;

    /**
     * Inner class to create Node objects
     */
    static class Node {
        /**
         * This node's NodeType based on the appropriate value
         */
        public NodeType nt;
        /**
         * Left and right pointer's of this node respectively
         */
        public Node left, right;
        /**
         * The value of this nude
         */
        public String value;

        /**
         * Default node creator, initializes all values to null
         */
        Node() {
            this.nt = null;
            this.left = null;
            this.right = null;
            this.value = null;
        }

        /**
         * Constructor for a Node type object
         * @param node_type the type of node
         * @param left left pointer of this node
         * @param right right pointer of this node
         * @param value the value of this node
         */
        Node(NodeType node_type, Node left, Node right, String value) {
            this.nt = node_type;
            this.left = left;
            this.right = right;
            this.value = value;
        }

        /**
         * Method to create a node object, calls the constructor
         * @param nodetype the type of node
         * @param left left pointer of this node
         * @param right right pointer of this node
         * @return a new node
         */
        public static Node make_node(NodeType nodetype, Node left, Node right) {
            return new Node(nodetype, left, right, "");
        }

        /**
         * Creates a node type object with only a left pointer and node type
         * @param nodetype the type of node
         * @param left left pointer of this node
         * @return a new node
         */
        public static Node make_node(NodeType nodetype, Node left) {
            return new Node(nodetype, left, null, "");
        }

        /**
         * Makes a leaf node with no children
         * @param nodetype the type of node
         * @param value the value of this node
         * @return a new leaf node
         */
        public static Node make_leaf(NodeType nodetype, String value) {
            return new Node(nodetype, null, null, value);
        }
    }

    /**
     * Private inner class to create Token object
     */
    static class Token {
        /**
         * The type of token; will be set based on an Enum of TokenTypes
         */
        public TokenType tokentype;
        /**
         * Value that this token will hold
         */
        public String value;
        /**
         * Line that this token is found on
         */
        public int line;
        /**
         * Position that this token is found in
         */
        public int pos;

        /**
         * Constructor for a Token object
         * @param token Type of Token for this object
         * @param value Value that this token will hold
         * @param line Line that this token can be found on
         * @param pos Position that this token can be found in
         */
        Token(TokenType token, String value, int line, int pos) {
            this.tokentype = token; this.value = value; this.line = line; this.pos = pos;
        }
        @Override
        public String toString() {
            return String.format("%5d  %5d %-15s %s", this.line, this.pos, this.tokentype, this.value);
        }
    }

    /**
     * Enum for preset TokenTypes that will be encountered within our subset of C code
     */
    static enum TokenType {
        End_of_input(false, false, false, -1, NodeType.nd_None),
        Op_multiply(false, true, false, 13, NodeType.nd_Mul),
        Op_divide(false, true, false, 13, NodeType.nd_Div),
        Op_mod(false, true, false, 13, NodeType.nd_Mod),
        Op_add(false, true, false, 12, NodeType.nd_Add),
        Op_subtract(false, true, false, 12, NodeType.nd_Sub),
        Op_negate(false, false, true, 14, NodeType.nd_Negate),
        Op_not(false, false, true, 14, NodeType.nd_Not),
        Op_less(false, true, false, 10, NodeType.nd_Lss),
        Op_lessequal(false, true, false, 10, NodeType.nd_Leq),
        Op_greater(false, true, false, 10, NodeType.nd_Gtr),
        Op_greaterequal(false, true, false, 10, NodeType.nd_Geq),
        Op_equal(false, true, true, 9, NodeType.nd_Eql),
        Op_notequal(false, true, false, 9, NodeType.nd_Neq),
        Op_assign(false, false, false, -1, NodeType.nd_Assign),
        Op_and(false, true, false, 5, NodeType.nd_And),
        Op_or(false, true, false, 4, NodeType.nd_Or),
        Keyword_if(false, false, false, -1, NodeType.nd_If),
        Keyword_else(false, false, false, -1, NodeType.nd_None),
        Keyword_while(false, false, false, -1, NodeType.nd_While),
        Keyword_print(false, false, false, -1, NodeType.nd_None),
        Keyword_putc(false, false, false, -1, NodeType.nd_None),
        LeftParen(false, false, false, -1, NodeType.nd_None),
        RightParen(false, false, false, -1, NodeType.nd_None),
        LeftBrace(false, false, false, -1, NodeType.nd_None),
        RightBrace(false, false, false, -1, NodeType.nd_None),
        Semicolon(false, false, false, -1, NodeType.nd_None),
        Comma(false, false, false, -1, NodeType.nd_None),
        Identifier(false, false, false, -1, NodeType.nd_Ident),
        Integer(false, false, false, -1, NodeType.nd_Integer),
        String(false, false, false, -1, NodeType.nd_String);

        /**
         * The precedence that this TokenType holds
         */
        private final int precedence;
        /**
         * Boolean whether this TokenType is right associative
         */
        private final boolean right_assoc;
        /**
         * Boolean value whether this TokenType is a binary operator
         */
        private final boolean is_binary;
        /**
         * Boolean value whether this TokenType is a unary operator
         */
        private final boolean is_unary;
        /**
         * Type of node this TokenType is
         */
        private final NodeType node_type;

        /**
         * Consturctor for a TokenType object
         * @param right_assoc Boolean, dependent on if this TokenType is right associative
         * @param is_binary Boolean, dependent on if this TokenType is binary
         * @param is_unary Boolean, dependent on if this TokenType is unary
         * @param precedence Precedence that this TokenType holds
         * @param node NodeType that this TokenType is
         */
        TokenType(boolean right_assoc, boolean is_binary, boolean is_unary, int precedence, NodeType node) {
            this.right_assoc = right_assoc;
            this.is_binary = is_binary;
            this.is_unary = is_unary;
            this.precedence = precedence;
            this.node_type = node;
        }

        /**
         * Getter method for right_assoc attribute
         * @return right_assoc
         */
        boolean isRightAssoc() { return this.right_assoc; }

        /**
         * Getter method for is_binary attribute
         * @return is_binary
         */
        boolean isBinary() { return this.is_binary; }

        /**
         * Getter method for is_unary attribute
         * @return is_unary
         */
        boolean isUnary() { return this.is_unary; }

        /**
         * Getter method for precedence attribute
         * @return precedence
         */
        int getPrecedence() { return this.precedence; }

        /**
         * Getter method for node_type attribute
         * @return node_type
         */
        NodeType getNodeType() { return this.node_type; }
    }

    /**
     * Enum of preset NodeTypes that we can possibly be encountered in our C subset
     */
    static enum NodeType {
        nd_None(""), nd_Ident("Identifier"), nd_String("String"), nd_Integer("Integer"), nd_Sequence("Sequence"), nd_If("If"),
        nd_Prtc("Prtc"), nd_Prts("Prts"), nd_Prti("Prti"), nd_While("While"),
        nd_Assign("Assign"), nd_Negate("Negate"), nd_Not("Not"), nd_Mul("Multiply"), nd_Div("Divide"), nd_Mod("Mod"), nd_Add("Add"),
        nd_Sub("Subtract"), nd_Lss("Less"), nd_Leq("LessEqual"),
        nd_Gtr("Greater"), nd_Geq("GreaterEqual"), nd_Eql("Equal"), nd_Neq("NotEqual"), nd_And("And"), nd_Or("Or");

        /**
         * Name of this NodeType
         */
        private final String name;

        /**
         * Constructor for a NodeType object
         * @param name
         */
        NodeType(String name) {
            this.name = name;
        }

        @Override
        public String toString() { return this.name; }
    }

    /**
     * Method to produce an error that halts the program and produces output to terminal
     * @param line line that this error was found on
     * @param pos position that this error was found in
     * @param msg additional information that would be outputted
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
     * Constructor for a Parser object
     * @param source a list of Tokens
     */
    Parser(List<Token> source) {
        this.source = source;
        this.token = null;
        this.position = 0;
    }

    /**
     * method to move the current token forward one and to return the next token
     * @return next token
     */
    Token getNextToken() {
        this.token = this.source.get(this.position++);
        return this.token;
    }

    /**
     * method to parse any expressions encountered
     * @param p precedence of this expression
     * @return result of the parse
     */
    Node expr(int p) {
        // create nodes for token types such as LeftParen, Op_add, Op_subtract, etc.
        // be very careful here and be aware of the precedence rules for the AST tree

        Node result = null, node;
        TokenType op;
        int precLvl;

        if (this.token.tokentype == TokenType.LeftParen) {
            result = paren_expr();

        } else if (this.token.tokentype == TokenType.Op_not) {
            op = this.token.tokentype;
            getNextToken();
            Node oneNode = expr(op.getPrecedence());
            result = Node.make_node(op.getNodeType(), oneNode, null);
        } else if (this.token.tokentype == TokenType.Identifier || this.token.tokentype == TokenType.Integer) {
            result = Node.make_leaf(this.token.tokentype.getNodeType(), this.token.value);
            getNextToken();
        }

        while (this.token.tokentype.is_binary && this.token.tokentype.getPrecedence() >= p) {
                op = this.token.tokentype;
                this.getNextToken();

                if (op.isRightAssoc()) {
                    precLvl = op.getPrecedence();
                } else {
                    precLvl = op.getPrecedence() + 1;
                }
                Node rightNode = expr(precLvl);
                result = Node.make_node(op.getNodeType(), result, rightNode);

        }

        return result;
    }

    /**
     * Method to handle the encounter of a parenthesis with a single expression inside
     * @return node found within the parenthesis expression
     */

    Node paren_expr() {
        expect("paren_expr", TokenType.LeftParen);
        Node node = expr(0);
        expect("paren_expr", TokenType.RightParen);
        return node;
    }

    /**
     * Method to handle what would be expected next based on the grammar rules used
     * @param msg message to be outputted if an error is encountered
     * @param s TokenType that is expected at this part of the program
     */
    void expect(String msg, TokenType s) {
        if (this.token.tokentype == s) {
            getNextToken();
            return;
        }
        error(this.token.line, this.token.pos, msg + ": Expecting '" + s + "', found: '" + this.token.tokentype + "'");
    }

    /**
     * Handles the parsing of a statement that is encountered by the program based on grammar rules
     * @return the result of parsing of the statement
     */
    Node stmt() {
        // this one handles TokenTypes such as Keyword_if, Keyword_else, nd_If, Keyword_print, etc.
        // also handles while, end of file, braces
        Node s, s2 = null, t = null, e, v;

        switch(this.token.tokentype) {
            case Keyword_if:
                getNextToken();
                e = paren_expr();
                s = stmt();

                if (this.token.tokentype == TokenType.Keyword_else)  {
                    getNextToken();
                    s2 = stmt();
                }
                Node sequenceNode = Node.make_node(NodeType.nd_Sequence, s, s2);
                t = Node.make_node(NodeType.nd_If, e, sequenceNode);
                break;

            // case Keyword_print is incomplete; and does not function properly. Will fix during second opportunity.
            case Keyword_print:
                getNextToken();
                expect("LeftParen", TokenType.LeftParen);


                s = null;
                // this handling of String only works for Hello, as String literals should be handled within Keyword_print
                // only
                if(this.token.tokentype == TokenType.String) {
                    s = Node.make_node(NodeType.nd_Prts, Node.make_leaf(NodeType.nd_String, this.token.value));
                    getNextToken();
                }

                Node nextPrintNode = null;
                NodeType nodeType = null;

                // in a properly functioning case, this would "trap" the print() statement assuming it had more than
                // one entry separated by commas
                while (this.token.tokentype != TokenType.RightParen && this.token.tokentype == TokenType.Comma) {

                    // if it is a string, handle it similar to outside the loop
                    if (this.token.tokentype == TokenType.String) {
                        s =  Node.make_leaf(NodeType.nd_String, this.token.value);

                    // if it was not a string, and you're printing an integer or identifier, call expr() to  handle it
                    } else {

                    }
                    // connect it back as a sequence node
                    nextPrintNode = Node.make_node(this.token.tokentype.node_type, s, nextPrintNode);
                    getNextToken();
                }

                // end of print should be expecting a right parenthesis and semicolon
                expect("RightParen", TokenType.RightParen);
                expect("Semicolon", TokenType.Semicolon);

                // connect everything that was in the print statement back to t
                t = Node.make_node(NodeType.nd_Sequence, null, s);
                break;


            case Keyword_putc:
                getNextToken();
                e = paren_expr();
                t = Node.make_node(NodeType.nd_Prtc, e, null);
                expect("Semicolon", TokenType.Semicolon);
                break;


            case Keyword_while:
                getNextToken();
                e = paren_expr();
                s = stmt();
                t = Node.make_node(NodeType.nd_While, e, s);
                break;

            case Semicolon:
                getNextToken();
                break;

            case Identifier:
                String name = this.token.value;
                getNextToken();

                if (this.token.tokentype == TokenType.Op_assign) {
                    getNextToken();
                    v = Node.make_leaf(NodeType.nd_Ident, name);
                    e = expr(0);
                    t = Node.make_node(NodeType.nd_Assign, v, e);
                }
                expect("Semicolon", TokenType.Semicolon);
                break;

            case LeftBrace:
                getNextToken();
                s = stmt();
                while (this.token.tokentype != TokenType.RightBrace) {
                    s2 = stmt();
                    s = Node.make_node(NodeType.nd_Sequence, s, s2);
                }
                expect("RightBrace", TokenType.RightBrace);
                t = Node.make_node(NodeType.nd_Sequence, null, s);
                break;

//            case String:
//                t = Node.make_leaf(NodeType.nd_String, this.token.value);
//                getNextToken();
//                break;


        }

        return t;
    }

    /**
     * begins the parsing of the series of tokens
     * @return completed result
     */
    Node parse() {
        Node t = null;
        getNextToken();
        while (this.token.tokentype != TokenType.End_of_input) {
            t = Node.make_node(NodeType.nd_Sequence, t, stmt());
        }
        return t;
    }

    /**
     * Prints out the AST based on the parsing of the tokens
     * @param t node that holds the tokens
     * @param sb creates the String of nodes
     * @return String output of the AST tree
     */
    String printAST(Node t, StringBuilder sb) {
        int i = 0;
        if (t == null) {
            sb.append(";");
            sb.append("\n");
            System.out.println(";");
        } else {
            sb.append(t.nt);
            System.out.printf("%-14s", t.nt);
            if (t.nt == NodeType.nd_Ident || t.nt == NodeType.nd_Integer || t.nt == NodeType.nd_String) {
                sb.append(" " + t.value);
                sb.append("\n");
                System.out.println(" " + t.value);
            } else {
                sb.append("\n");
                System.out.println();
                printAST(t.left, sb);
                printAST(t.right, sb);
            }

        }
        return sb.toString();
    }

    /**
     * Outputs the results of parsing of the tokens to a file
     * @param result results of the parsing of the tokens
     */
    static void outputToFile(String result) {
        try {
            FileWriter myWriter = new FileWriter("src/main/resources/hello.par");
            myWriter.write(result);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runnable main method that allows program to execute and calls methods as needed
     */

    public static void main(String[] args) {
        if (1==1) {
            try {
                String value, token;
                String result = " ";
                StringBuilder sb = new StringBuilder();
                int line, pos;
                Token t;
                boolean found;
                List<Token> list = new ArrayList<>();
                Map<String, TokenType> str_to_tokens = new HashMap<>();


                str_to_tokens.put("LeftParen",TokenType.LeftParen );
                str_to_tokens.put("RightParen",TokenType.RightParen );
                str_to_tokens.put("Identifier",TokenType.Identifier );
                str_to_tokens.put("Integer",TokenType.Integer );
                str_to_tokens.put("Comma",TokenType.Comma );
                str_to_tokens.put("Keyword_if",TokenType.Keyword_if );
                str_to_tokens.put("Keyword_else",TokenType.Keyword_else );
                str_to_tokens.put("Keyword_print",TokenType.Keyword_print );
                str_to_tokens.put("Keyword_putc",TokenType.Keyword_putc );
                str_to_tokens.put("Keyword_while",TokenType.Keyword_while );
                str_to_tokens.put("LeftBrace",TokenType.LeftBrace );
                str_to_tokens.put("Op_add",TokenType.Op_add );
                str_to_tokens.put("Op_subtract",TokenType.Op_subtract );
                str_to_tokens.put("Op_and",TokenType.Op_and );
                str_to_tokens.put("Op_assign",TokenType.Op_assign );
                str_to_tokens.put("Op_greater",TokenType.Op_greater );
                str_to_tokens.put("Op_greaterequal",TokenType.Op_greaterequal );
                str_to_tokens.put("Op_equal",TokenType.Op_equal );
                str_to_tokens.put("Op_lessequal",TokenType.Op_lessequal );
                str_to_tokens.put("Op_less",TokenType.Op_less );
                str_to_tokens.put("Op_mod",TokenType.Op_mod );
                str_to_tokens.put("Op_multiply",TokenType.Op_multiply );
                str_to_tokens.put("Op_divide",TokenType.Op_divide );
                str_to_tokens.put("Op_negate",TokenType.Op_negate );
                str_to_tokens.put("Op_not",TokenType.Op_not );
                str_to_tokens.put("Op_notequal",TokenType.Op_notequal );
                str_to_tokens.put("Op_or",TokenType.Op_or );
                str_to_tokens.put("RightBrace",TokenType.RightBrace );
                str_to_tokens.put("String",TokenType.String );
                str_to_tokens.put("Semicolon",TokenType.Semicolon );
                str_to_tokens.put("End_of_input", TokenType.End_of_input);

                // finish creating your Hashmap. I left one as a model

                Scanner s = new Scanner(new File("src/main/resources/hello.lex"));
                String source = " ";
                while (s.hasNext()) {
                    String str = s.nextLine();
                    StringTokenizer st = new StringTokenizer(str);
                    line = Integer.parseInt(st.nextToken());
                    pos = Integer.parseInt(st.nextToken());
                    token = st.nextToken();
                    value = "";
                    while (st.hasMoreTokens()) {
                        value += st.nextToken() + " ";
                    }
                    found = false;
                    if (str_to_tokens.containsKey(token)) {
                        found = true;
                        list.add(new Token(str_to_tokens.get(token), value, line, pos));
                    }
                    if (found == false) {
                        throw new Exception("Token not found: '" + token + "'");
                    }
                }
                Parser p = new Parser(list);
                result = p.printAST(p.parse(), sb);
                outputToFile(result);
            } catch (FileNotFoundException e) {
                error(-1, -1, "Exception: " + e.getMessage());
            } catch (Exception e) {
                error(-1, -1, "Exception: " + e.getMessage());
            }
        } else {
            error(-1, -1, "No args");
        }
    }
}