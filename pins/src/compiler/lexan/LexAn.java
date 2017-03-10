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
        // TODO
        this.dump = dump;
        try {
            this.scn = new Scanner(new File(sourceFileName));
            this.scn.useDelimiter("");

        } catch (FileNotFoundException e) {
            System.out.println(e);
            System.exit(1);
        }

        begLine = 1;
        begColumn = 1;
        endLine = 1;
        endColumn = 1;
        this.next = "";
    }

    /**
     * Vrne naslednji simbol iz izvorne datoteke. Preden vrne simbol, ga izpise
     * na datoteko z vmesnimi rezultati.
     *
     * @return Naslednji simbol iz izvorne datoteke.
     */
    public Symbol lexAn() {
        String buffer = "";
        Position position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);

        while (scn.hasNext() || this.next.length() > 0) {

            if (this.next.length() > 0) {
                buffer += this.next;
                this.next = "";
            } else {
                buffer += scn.next();
            }

            position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
            Symbol s = null;

            /** new line */
            if (buffer.equals((char) 10 + "") || buffer.equals((char) 13 + "")) {
                this.newLine();
                buffer = "";
            }
            /** space in tab */
            else if (buffer.equals((char) 9 + "") || buffer.equals((char) 32 + "")) {
                this.updatePosition();
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
                if (this.scn.hasNext()) {
                    this.next = scn.next();
                }
                if (this.next.equals("=")) {
                    buffer += this.next;
                    this.next = "";
                    setPosition(buffer.length());
                    position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                    s = new Symbol(Token.NEQ, buffer, position);
                } else {
                    s = new Symbol(Token.NOT, buffer, position);
                }

            }
            /** = in == operator */
            // TODO: separate = and == ?
            else if (buffer.equals("=")) {
                if (this.scn.hasNext()) {
                    this.next = scn.next();
                }
                if (this.next.equals("=")) {
                    buffer += this.next;
                    this.next = "";
                    setPosition(buffer.length());
                    position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                    s = new Symbol(Token.EQU, buffer, position);
                } else {
                    s = new Symbol(Token.ASSIGN, buffer, position);
                }
            }
            /** < in <= operator */
            // TODO: separate < and <= ?
            else if (buffer.equals("<")) {
                if (this.scn.hasNext()) {
                    this.next = scn.next();
                }
                if (this.next.equals("=")) {
                    buffer += this.next;
                    this.next = "";
                    setPosition(buffer.length());
                    position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                    s = new Symbol(Token.LEQ, buffer, position);
                } else {
                    s = new Symbol(Token.LTH, buffer, position);
                }
            }
            /** > in => operator */
            // TODO: separate = and == ?
            else if (buffer.equals(">")) {
                if (this.scn.hasNext()) {
                    this.next = scn.next();
                }
                if (this.next.equals("=")) {
                    buffer += this.next;
                    this.next = "";
                    setPosition(buffer.length());
                    position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
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
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_ARR, buffer, position);
            }
            /** kljucna beseda else */
            else if (buffer.equals("else")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_ELSE, buffer, position);
            }
            /** kljucna beseda for */
            else if (buffer.equals("for")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_FOR, buffer, position);
            }
            /** kljucna beseda fun */
            else if (buffer.equals("fun")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_FUN, buffer, position);
            }
            /** kljucna beseda if */
            else if (buffer.equals("if")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_IF, buffer, position);
            }
            /** kljucna beseda then */
            else if (buffer.equals("then")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_THEN, buffer, position);
            }
            /** kljucna beseda typ */
            else if (buffer.equals("typ")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_TYP, buffer, position);
            }
            /** kljucna beseda var */
            else if (buffer.equals("var")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_VAR, buffer, position);
            }
            /** kljucna beseda where */
            else if (buffer.equals("where")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_WHERE, buffer, position);
            }
            /** kljucna beseda while */
            else if (buffer.equals("while")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.KW_WHILE, buffer, position);
            }
            /** tip logical (true, false) */
            else if (buffer.equals("true") || buffer.equals("false")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.LOG_CONST, buffer, position);
            }
            /** tip integer */
            else if (buffer.matches("[0-9]+")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[0-9]+")) {
                        continue;
                    }
                }
                this.setPosition(buffer.length());
                position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                s = new Symbol(Token.INT_CONST, buffer, position);
            }
            /** tip string */
            else if (buffer.matches("\'[^\']*")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.equals("'")) {
                        buffer += this.next;
                        this.next = "";
                        this.setStringPosition(buffer);
                        position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                        s = new Symbol(Token.STR_CONST, buffer, position);
                    }
                } else {
                    this.setStringPosition(buffer);
                    position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                    Report.error(position, "String ni zakljucen!");
                    System.exit(1);
                }
            }
            /** identifier */
            else if (buffer.matches("[a-zA-Z]+[a-zA-Z0-9_]*")) {
                if (this.scn.hasNext()) {
                    this.next = this.scn.next();
                    if (this.next.matches("[a-zA-Z0-9_]+")) {
                        continue;
                    } else {
                        this.setPosition(buffer.length());
                        position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                        s = new Symbol(Token.IDENTIFIER, buffer, position);
                    }
                } else {
                    this.setPosition(buffer.length());
                    position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
                    s = new Symbol(Token.IDENTIFIER, buffer, position);
                }
            }

            if (s != null) {
                this.updatePosition();
                System.out.println(s + " " + s.position);
                return s;
            }
        }

        position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
        this.next = "";
        buffer = "";
        return new Symbol(Token.EOF, buffer, position);
    }

    /**
     * Posodobi pozicijo na zacetek novega simbola
     */
    private void updatePosition() {
        this.endColumn += 1;
        this.begColumn = endColumn;
    }

    /**
     * Nastavi pozicijo od začetka do konca simbola
     *
     * @param len dolžina simbola
     */
    private void setPosition(int len) {
        this.endColumn = this.begColumn + len - 1;
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
                this.endLine += 1;
                this.endColumn = 1;
            } else {
                this.endColumn += 1;
            }
        }
        if (this.endColumn > 1) {
            this.endColumn -= 1;
        }
    }

    /**
     * Nastavi pozicijo ob skoku v novo vrstico
     */
    private void newLine() {
        this.begColumn = 1;
        this.begLine += 1;
        this.endColumn = 1;
        this.endLine += 1;
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
