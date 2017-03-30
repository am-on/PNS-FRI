package compiler.lexan;

import compiler.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InterruptedIOException;
import java.util.Scanner;

/**
 * Leksikalni analizator.
 *
 * @author sliva
 */
public class LexAn {

    /**
     * Ali se izpisujejo vmesni rezultati.
     */
    private boolean dump;

    /**
     * File scanner
     */
    private Scanner scn;

    /**
     * Trenutna pozicija
     */
    private int begLine;
    private int begColumn;
    int endLine;
    int endColumn;


    /**
     * Hranjenje naslednjega znaka iz datoteke
     */
    private String next;


    /**
     * Ustvari nov leksikalni analizator.
     *
     * @param sourceFileName Ime izvorne datoteke.
     * @param dump           Ali se izpisujejo vmesni rezultati.
     */
    public LexAn(String sourceFileName, boolean dump) {
        this.dump = dump;
        try {
            scn = new Scanner(new File(sourceFileName));
            scn.useDelimiter("");

        } catch (FileNotFoundException e) {
            System.out.println(e);
            System.exit(1);
        }

        begLine = 1;
        begColumn = 1;
        endLine = 1;
        endColumn = 1;
        next = "";
    }

    /**
     * Vrne naslednji simbol iz izvorne datoteke. Preden vrne simbol, ga izpise
     * na datoteko z vmesnimi rezultati.
     *
     * @return Naslednji simbol iz izvorne datoteke.
     */
    public Symbol lexAn() {
        Position position;
        Symbol s;
        String buffer = "";

        while (scn.hasNext() || next.length() > 0) {

            if (next.length() > 0) {
                buffer += next;
                next = "";
            } else {
                buffer += scn.next();
            }

            s = null;
            position = new Position(begLine, begColumn, endLine, endColumn);


            /** new line */
            if (buffer.equals((char) 10 + "") || buffer.equals((char) 13 + "")) {
                newLine();
                buffer = "";
            }
            /** space in tab */
            else if (buffer.equals((char) 9 + "") || buffer.equals((char) 32 + "")) {
                updatePosition();
                buffer = "";
            }
            /** ADD operator */
            else if (buffer.equals("+")) {
                s = new Symbol(Token.ADD, buffer, position);
            }
            /** SUB operator */
            else if (buffer.equals("-")) {
                s = new Symbol(Token.SUB, buffer, position);
            }
            /** MUL operator */
            else if (buffer.equals("*")) {
                s = new Symbol(Token.MUL, buffer, position);
            }
            /** DIV operator */
            else if (buffer.equals("/")) {
                s = new Symbol(Token.DIV, buffer, position);
            }
            /** MOD operator */
            else if (buffer.equals("%")) {
                s = new Symbol(Token.MOD, buffer, position);
            }
            /** AND operator */
            else if (buffer.equals("&")) {
                s = new Symbol(Token.AND, buffer, position);
            }
            /** OR operator */
            else if (buffer.equals("|")) {
                s = new Symbol(Token.IOR, buffer, position);
            }
            /** ! in != operator */
            // TODO: separate ! and != ?
            else if (buffer.equals("!")) {
                if (scn.hasNext()) {
                    next = scn.next();
                }
                if (next.equals("=")) {
                    buffer += next;
                    next = "";
                    setPosition(buffer.length());
                    position = new Position(begLine, begColumn, endLine, endColumn);
                    s = new Symbol(Token.NEQ, buffer, position);
                } else {
                    s = new Symbol(Token.NOT, buffer, position);
                }

            }
            /** = in == operator */
            // TODO: separate = and == ?
            else if (buffer.equals("=")) {
                if (scn.hasNext()) {
                    next = scn.next();
                }
                if (next.equals("=")) {
                    buffer += next;
                    next = "";
                    setPosition(buffer.length());
                    position = new Position(begLine, begColumn, endLine, endColumn);
                    s = new Symbol(Token.EQU, buffer, position);
                } else {
                    s = new Symbol(Token.ASSIGN, buffer, position);
                }
            }
            /** < in <= operator */
            // TODO: separate < and <= ?
            else if (buffer.equals("<")) {
                if (scn.hasNext()) {
                    next = scn.next();
                }
                if (next.equals("=")) {
                    buffer += next;
                    next = "";
                    setPosition(buffer.length());
                    position = new Position(begLine, begColumn, endLine, endColumn);
                    s = new Symbol(Token.LEQ, buffer, position);
                } else {
                    s = new Symbol(Token.LTH, buffer, position);
                }
            }
            /** > in => operator */
            // TODO: separate = and == ?
            else if (buffer.equals(">")) {
                if (scn.hasNext()) {
                    next = scn.next();
                }
                if (next.equals("=")) {
                    buffer += next;
                    next = "";
                    setPosition(buffer.length());
                    position = new Position(begLine, begColumn, endLine, endColumn);
                    s = new Symbol(Token.GEQ, buffer, position);
                } else {
                    s = new Symbol(Token.GTH, buffer, position);
                }
            }
            /** levi oklepaj */
            else if (buffer.equals("(")) {
                s = new Symbol(Token.LPARENT, buffer, position);
            }
            /** desni oklepaj */
            else if (buffer.equals(")")) {
                s = new Symbol(Token.RPARENT, buffer, position);
            }
            /** levi oglati oklepaj */
            else if (buffer.equals("[")) {
                s = new Symbol(Token.LBRACKET, buffer, position);
            }
            /** desni oglati oklepaj */
            else if (buffer.equals("]")) {
                s = new Symbol(Token.RBRACKET, buffer, position);
            }
            /** levi zaviti oklepaj */
            else if (buffer.equals("{")) {
                s = new Symbol(Token.LBRACE, buffer, position);
            }
            /** desni zaviti oklepaj */
            else if (buffer.equals("}")) {
                s = new Symbol(Token.RBRACE, buffer, position);
            }
            /** dvopicje */
            else if (buffer.equals(":")) {
                s = new Symbol(Token.COLON, buffer, position);
            }
            /** podpicje */
            else if (buffer.equals(";")) {
                s = new Symbol(Token.SEMIC, buffer, position);
            }
            /** pika */
            else if (buffer.equals(".")) {
                s = new Symbol(Token.DOT, buffer, position);
            }
            /** vejica */
            else if (buffer.equals(",")) {
                s = new Symbol(Token.COMMA, buffer, position);
            }
            /** kljucna beseda arr */
            else if (buffer.equals("arr")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_ARR, buffer, position);
            }
            /** kljucna beseda else */
            else if (buffer.equals("else")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_ELSE, buffer, position);
            }
            /** kljucna beseda for */
            else if (buffer.equals("for")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_FOR, buffer, position);
            }
            /** kljucna beseda fun */
            else if (buffer.equals("fun")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_FUN, buffer, position);
            }
            /** kljucna beseda if */
            else if (buffer.equals("if")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_IF, buffer, position);
            }
            /** kljucna beseda then */
            else if (buffer.equals("then")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_THEN, buffer, position);
            }
            /** kljucna beseda typ */
            else if (buffer.equals("typ")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_TYP, buffer, position);
            }
            /** kljucna beseda var */
            else if (buffer.equals("var")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_VAR, buffer, position);
            }
            /** kljucna beseda where */
            else if (buffer.equals("where")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_WHERE, buffer, position);
            }
            /** kljucna beseda while */
            else if (buffer.equals("while")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.KW_WHILE, buffer, position);
            }
            /** ime atomarnega podatkovnega tipa logical */
            else if (buffer.equals("logical")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.LOGICAL, buffer, position);
            }
            /** ime atomarnega podatkovnega tipa integer */
            else if (buffer.equals("integer")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.INTEGER, buffer, position);
            }
            /** ime atomarnega podatkovnega tipa string */
            else if (buffer.equals("string")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.STRING, buffer, position);
            }
            /** tip logical (true, false) */
            else if (buffer.equals("true") || buffer.equals("false")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.LOG_CONST, buffer, position);
            }
            /** tip integer */
            else if (buffer.matches("[0-9]+")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[0-9]+")) {
                        continue;
                    }
                }
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                s = new Symbol(Token.INT_CONST, buffer, position);
            }
            /** tip string */
            else if (buffer.charAt(0) == '\'') {
                if (scn.hasNext()) {
                    next = scn.next();

                    if (next.equals("'")) {
                        buffer += next;
                        next = "";

                        // preveri, ce gre slucajno za ''
                        if(scn.hasNext()) {
                            next = scn.next();
                            if (next.equals("'")) {
                                continue;
                            }
                        }

                        setStringPosition(buffer);
                        position = new Position(begLine, begColumn, endLine, endColumn);
                        s = new Symbol(Token.STR_CONST, buffer, position);

                    } else if (next.charAt(0) < 32 || next.charAt(0) > 126) {
                        setPosition(buffer.length());
                        position = new Position(begLine, begColumn, endLine, endColumn);
                        if(next.charAt(0) == 9 || next.charAt(0) == 10 || next.charAt(0) == 13) {
                            Report.error(position, "Unclosed string const.");
                        }
                        updatePosition();
                        position = new Position(begLine, begColumn, endLine, endColumn);
                        Report.error(position, "Invalid char \'" + next + "\' " + "in string const.");
                    }
                } else {
                    setPosition(buffer.length());
                    position = new Position(begLine, begColumn, endLine, endColumn);
                    Report.error(position, "Unclosed string const. Expected ', got: EOF");
                }
            }
            /** identifier */
            else if (buffer.matches("[a-zA-Z_]+[a-zA-Z0-9_]*")) {
                if (scn.hasNext()) {
                    next = scn.next();
                    if (next.matches("[a-zA-Z0-9_]+")) {
                        continue;
                    } else {
                        setPosition(buffer.length());
                        position = new Position(begLine, begColumn, endLine, endColumn);
                        s = new Symbol(Token.IDENTIFIER, buffer, position);
                    }
                } else {
                    setPosition(buffer.length());
                    position = new Position(begLine, begColumn, endLine, endColumn);
                    s = new Symbol(Token.IDENTIFIER, buffer, position);
                }
            }
            /** komentar # */
            else if (buffer.charAt(0) == 35) {
                if(scn.hasNext()) {
                    next = scn.next();
                    if(next.charAt(0) == 10 || next.charAt(0) == 13) {
                        buffer = "";
                    }
                }
            }
            /** neznani simbol */
            else {
                setPosition(buffer.length());
                position = new Position(begLine, begColumn, endLine, endColumn);
                Report.error(position, "Invalid char \'" + buffer + "\'");
            }

            if (s != null) {
                updatePosition();
                dump(s);
                return s;
            }
        }

        position = new Position(begLine, begColumn, endLine, endColumn);
        next = "";
        buffer = "";

        s = new Symbol(Token.EOF, buffer, position);
        dump(s);
        return new Symbol(Token.EOF, buffer, position);
    }

    /**
     * Posodobi pozicijo na zacetek novega simbola
     */
    private void updatePosition() {
        endColumn += 1;
        begColumn = endColumn;
    }

    /**
     * Nastavi pozicijo od začetka do konca simbola
     *
     * @param len dolžina simbola
     */
    private void setPosition(int len) {
        endColumn = begColumn + len - 1;
    }

    /**
     * Nastavi pozicijo od začetka do konca simbola
     *
     * @param str vsebina stringa
     */
    private void setStringPosition(String str) {
        for (int i = 0; i < str.length(); i++) {
            int chr = str.charAt(i);
            if (chr == 10 || chr == 13) {
                endLine += 1;
                endColumn = 1;
            } else {
                endColumn += 1;
            }
        }
        if (endColumn > 1) {
            endColumn -= 1;
        }
    }

    /**
     * Nastavi pozicijo ob skoku v novo vrstico
     */
    private void newLine() {
        begColumn = 1;
        begLine += 1;
        endColumn = 1;
        endLine += 1;
    }


    /**
     * Izpise simbol v datoteko z vmesnimi rezultati.
     *
     * @param symb Simbol, ki naj bo izpisan.
     */
    private void dump(Symbol symb) {
        if (!dump) return;
        if (Report.dumpFile() == null) return;
        if (symb.token == Token.EOF)
            Report.dumpFile().println(symb.toString());
        else
            Report.dumpFile().println("[" + symb.position.toString() + "] " + symb.toString());
    }

}
