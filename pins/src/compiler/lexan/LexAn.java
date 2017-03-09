package compiler.lexan;

import compiler.*;

import java.util.Scanner;

/**
 * Leksikalni analizator.
 * 
 * @author sliva
 */
public class LexAn {
	
	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/** File scanner*/
	private Scanner scn;

	/** track position */
	private int begLine;
	private int begColumn;
	int endLine;
	int endColumn;


	/** Buffer for storing next value */
	private String next;


	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param sourceFileName
	 *            Ime izvorne datoteke.
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public LexAn(String sourceFileName, boolean dump) {		
		// TODO
		this.dump = dump;
		this.scn = new Scanner(sourceFileName);
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
		if(scn.hasNext()) {
			while (scn.hasNext() || this.next.length() > 0) {

				buffer += scn.next();
				if(scn.hasNext())
				this.next = scn.next();

				position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);

				switch(buffer) {
					case "+":
						updatePosition();
						return new Symbol(Token.ADD, buffer, position);
						break;
					case "-":
						updatePosition();
						return new Symbol(Token.SUB, buffer, position);
						break;
					case "*":
						updatePosition();
						return new Symbol(Token.MUL, buffer, position);
						break;
					case "/":
						updatePosition();
						return new Symbol(Token.DIV, buffer, position);
						break;
					case "%":
						updatePosition();
						return new Symbol(Token.MOD, buffer, position);
						break;
					case "&":
						updatePosition();
						return new Symbol(Token.AND, buffer, position);
						break;
					case "|":
						updatePosition();
						return new Symbol(Token.IOR, buffer, position);
						break;
					case "!":
						if (this.scn.hasNext()) {
							this.next = scn.next();
						}
						if(this.next.equals("=")) {
							buffer += this.next;
							this.next = "";
							setPosition(buffer.length());
							position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
							updatePosition();
							return new Symbol(Token.NEQ, buffer, position);
						}
						updatePosition();
						return new Symbol(Token.NOT, buffer, position);
						break;
					case "=":
						if (this.scn.hasNext()) {
							this.next = scn.next();
						}
						if(this.next.equals("=")) {
							buffer += this.next;
							this.next = "";
							setPosition(buffer.length());
							position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
							updatePosition();
							return new Symbol(Token.EQU, buffer, position);
						} else {
							return new Symbol(Token.ASSIGN, buffer, position);
						}
						break;
					case "<":
						if (this.scn.hasNext()) {
							this.next = scn.next();
						}
						if(this.next.equals("=")) {
							buffer += this.next;
							this.next = "";
							setPosition(buffer.length());
							position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
							updatePosition();
							return new Symbol(Token.LEQ, buffer, position);
						}
						return new Symbol(Token.LTH, buffer, position);
					break;
					case ">":
						if (this.scn.hasNext()) {
							this.next = scn.next();
						}
						if(this.next.equals("=")) {
							buffer += this.next;
							this.next = "";
							setPosition(buffer.length());
							position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
							updatePosition();
							return new Symbol(Token.GEQ, buffer, position);
						}
						updatePosition();
						return new Symbol(Token.GTH, buffer, position);
						break;
					case "(":
						updatePosition();
						return new Symbol(Token.LPARENT, buffer, position);
						break;
					case ")":
						updatePosition();
						return new Symbol(Token.RPARENT, buffer, position);
						break;
					case "[":
						updatePosition();
						return new Symbol(Token.LBRACKET, buffer, position);
					break;
					case "]":
						updatePosition();
						return new Symbol(Token.RBRACKET, buffer, position);
						break;
					case "{":
						updatePosition();
						return new Symbol(Token.LBRACE, buffer, position);
						break;
					case "}":
						updatePosition();
						return new Symbol(Token.RBRACE, buffer, position);
						break;
					case ":":
						updatePosition();
						return new Symbol(Token.COLON, buffer, position);
						break;
					case ";":
						updatePosition();
						return new Symbol(Token.SEMIC, buffer, position);
						break;
					case ".":
						updatePosition();
						return new Symbol(Token.DOT, buffer, position);
						break;
					case ",":
						updatePosition();
						return new Symbol(Token.COMMA, buffer, position);
						break;
					case "arr":
						if (this.scn.hasNext()) {
							this.next = scn.next();
						}
						if(this.next.matches("[a-z][A-Z][0-9]_")) {
							continue;
						} else {
							// todo popravi position!!!
							return new Symbol(Token.KW_ARR, buffer, position);
						}


					// arr else for fun if then typ var where while
					// + - * / % & | ! == != < > <= >= ( ) [ ] { } : ; . , =
				}
			}


		} else {
			position = new Position(this.begLine, this.begColumn, this.endLine, this.endColumn);
			this.next = "";
			return new Symbol(Token.EOF, "", position);
		}

	}

	/**
	 *  Posodobi pozicijo na zacetek novega simbola
	 */
	private void updatePosition() {
		this.endLine += 1;
		this.begLine = endLine;
	}

	/**
	 * Nastavi pozicijo od začetka do konca simbola
	 * @param len
	 * 		dolžina simbola
	 */
	private void setPosition(int len) {
		this.endLine = this.begLine + len - 1;
	}

	/**
	 *  Nastavi pozicijo ob skoku v novo vrstico
	 */
	private void newLine() {
		this.begColumn = 0;
		this.begLine += 1;
		this.endColumn = 0;
		this.endLine += 1;
	}


	/**
	 * Izpise simbol v datoteko z vmesnimi rezultati.
	 * 
	 * @param symb
	 *            Simbol, ki naj bo izpisan.
	 */
	private void dump(Symbol symb) {
		if (! dump) return;
		if (Report.dumpFile() == null) return;
		if (symb.token == Token.EOF)
			Report.dumpFile().println(symb.toString());
		else
			Report.dumpFile().println("[" + symb.position.toString() + "] " + symb.toString());
	}

}
