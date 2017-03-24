package compiler.synan;

import compiler.Report;
import compiler.lexan.*;

/**
 * Sintaksni analizator.
 * 
 * @author sliva
 */
public class SynAn {

	/** Leksikalni analizator. */
	private LexAn lexAn;

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	private int next;

    private Symbol nextSym;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param lexAn
	 *            Leksikalni analizator.
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public SynAn(LexAn lexAn, boolean dump) {
		this.lexAn = lexAn;
		this.dump = dump;
		this.next = -1;
	}

	private int nextToken() {
		if (this.next == -1) {
            return lexAn.lexAn().token;
        }
		int next = this.next;
		this.next = -1;
		return next;
	}

	private Symbol nextSymbol() {
        return lexAn.lexAn();
    }

	private int peek() {
		if (this.next == -1) {
		    this.nextSym = lexAn.lexAn();
			this.next = nextSym.token;
		}
		return this.next;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {


        dump("source -> definitions");
        parseDefinitions();

        if(peek() != Token.EOF) {
            Report.error("ups");
        }

	}

    /**
     * definitions -> definition definitions' .
     * */
	private boolean parseDefinitions() {
        dump("definitions -> definition definitions'");
		return parseDefinition() && parseDefinitions_();
	}

    /**
     * definition -> type_definition .
     * definition -> function_definition .
     * definition -> variable_definition .
     */
    private boolean parseDefinition() {
        switch (peek()) {
            case Token.KW_TYP:
                dump("definition -> type_definition");
                return parseTypeDefinition();
            case Token.KW_FUN:
                dump("definition -> function_definition");
                return parseFunctionDefinition();
            case Token.KW_VAR:
                dump("definition -> variable_definition");
                return parseVariableDefinition();
            default: Report.error("Invalid type or variable definition");
        }
        return false;
    }

    /**
     * definitions' -> ; definition definitions' .
     * definitions' -> .
     */
    private boolean parseDefinitions_() {
        dump("definitions' -> ; definition definitions'");
        if (peek() == Token.SEMIC) {
            nextToken();
            return parseDefinition() && parseDefinitions_();
        }
        dump("definitions' -> ε");
        return true;
    }

    /**
     * type_definition -> typ identifier : type
     */
    private boolean parseTypeDefinition() {
        dump("type_definition -> typ identifier : type");
        return parseEndSymbol(Token.KW_TYP)
                && parseEndSymbol(Token.IDENTIFIER)
                && parseEndSymbol(Token.COLON)
                && parseType();

    }

    /**
     * function_definition -> fun identifier ( parameters ) : type = expression .
     */
    private boolean parseFunctionDefinition() {
        dump("function_definition -> fun identifier ( parameters ) : type = expression");

        return parseEndSymbol(Token.KW_FUN)
                && parseEndSymbol(Token.IDENTIFIER)
                && parseEndSymbol(Token.LPARENT)
                && parseParameters()
                && parseEndSymbol(Token.RPARENT)
                && parseEndSymbol(Token.COLON)
                && parseType()
                && parseEndSymbol(Token.ASSIGN)
                && parseExpression();
    }

    /**
     * variable_definition -> var identifier : type
     */
    private boolean parseVariableDefinition() {
        dump("variable_definition -> var identifier : type");
        return parseEndSymbol(Token.KW_VAR)
                && parseEndSymbol(Token.IDENTIFIER)
                && parseEndSymbol(Token.COLON)
                && parseType();
    }

    /**
     * type -> identifier .
     * type -> logical .
     * type -> integer .
     * type -> string .
     * type -> arr [ int_const ] type .
     *
     */
    private boolean parseType() {
        switch (peek()) {
            case Token.IDENTIFIER:
                dump("type -> identifier");
                return parseEndSymbol(Token.IDENTIFIER);
            case Token.LOGICAL:
                dump("type -> logical");
                return parseEndSymbol(Token.LOGICAL);
            case Token.INTEGER:
                dump("type -> integer");
                return parseEndSymbol(Token.INTEGER);
            case Token.STRING:
                dump("type -> string");
                return parseEndSymbol(Token.STRING);
            case Token.KW_ARR:
                dump("type -> arr [ int_const ] type");

                return parseEndSymbol(Token.KW_ARR)
                        && parseEndSymbol(Token.LBRACKET)
                        && parseEndSymbol(Token.INT_CONST)
                        && parseEndSymbol(Token.RBRACKET)
                        && parseType();
            default:
                Report.error(nextSym.position, "Invalid type name: " + this.nextSym.toString());
                return false;
        }

    }

    /**
     * parameters -> parameter parameters' .
     */
    private boolean parseParameters() {
        dump("parameters -> parameter parameters'");
        return parseParameter() && parseParameters_();
    }

    /**
     * parameter -> identifier : type .
     */
    private boolean parseParameter() {
        dump("parameter -> identifier : type");

        return parseEndSymbol(Token.IDENTIFIER)
                && parseEndSymbol(Token.COLON)
                && parseType();

    }

    /**
     * parameters' -> , parameter parameters' .
     * parameters' -> .
     */
    private boolean parseParameters_() {
        if (peek() == Token.COMMA) {
            return parseEndSymbol(Token.COMMA) && parseParameter() && parseParameters_();
        }

        dump("parameters' -> ε");
        return true;
    }

    /**
     * expression -> logical_ior_expression expression'
     */
    private boolean parseExpression() {
        return parseLogicalIorExpression() && parseExpression_();
    }

    /**
     * expression' -> { WHERE definitions }
     * expression' -> .
     */
    private boolean parseExpression_() {
        if (peek() == Token.LBRACE) {
            dump("expression' -> { WHERE definitions }");
            return parseEndSymbol(Token.LBRACE)
                    && parseEndSymbol(Token.KW_WHERE)
                    && parseDefinitions()
                    && parseEndSymbol(Token.RBRACE);
        }
        dump("expression' -> ε");
        return true;
    }

    /**
     * logical_ior_expression -> logical_and_expression logical_ior_expression'     *
     */
    private boolean parseLogicalIorExpression() {
        return parseLogicalAndExpression() && parseLogicalIorExpression_();
    }

    /**
     * logical_ior_expression' -> | logical_and_expression logical_ior_expression' .
     * logical_ior_expression' -> .
     * @return
     */
    private boolean parseLogicalIorExpression_() {
        if (peek() == Token.IOR) {
            dump("logical_ior_expression' -> | logical_and_expression logical_ior_expression'");
            return parseEndSymbol(Token.IOR) && parseLogicalAndExpression() && parseLogicalIorExpression_();
        }
        dump("logical_ior_expression' -> ε");
        return true;
    }

    /**
     * logical_and_expression -> compare_expression logical_and_expression'
     */
    private boolean parseLogicalAndExpression() {
        dump("logical_and_expression -> compare_expression logical_and_expression'");
        return parseCompareExpression() && parseLogicalAndExpression_();
    }

    /**
     * logical_and_expression' -> & compare_expression logical_and_expression'
     * logical_and_expression' -> .
     */
    private boolean parseLogicalAndExpression_() {
        if (peek() == Token.AND) {
            dump("logical_and_expression' -> & compare_expression logical_and_expression'");
            return parseEndSymbol(Token.AND)
                    && parseCompareExpression()
                    && parseLogicalAndExpression_();
        }
        dump("logical_and_expression' -> ε");
        return true;
    }

    /**
     * compare_expression -> additive_expression compare_expression'
     */
    private boolean parseCompareExpression() {
        dump("compare_expression -> additive_expression compare_expression'");
        return parseAdditiveExpression()
                && parseCompareExpression_();
    }

    /**
     * compare_expression' -> == additive_expression .
     * compare_expression' -> != additive_expression .
     * compare_expression' -> <= additive_expression .
     * compare_expression' -> >= additive_expression .
     * compare_expression' -> < additive_expression .
     * compare_expression' -> > additive_expression .
     * compare_expression' -> .
     */
    private boolean parseCompareExpression_() {
        switch (peek()) {
            case Token.EQU:
                dump("compare_expression' -> == additive_expression");
                return parseEndSymbol(Token.EQU) && parseAdditiveExpression();
            case Token.NEQ:
                dump("compare_expression' -> != additive_expression");
                return parseEndSymbol(Token.NEQ) && parseAdditiveExpression();
            case Token.LEQ:
                dump("compare_expression' -> <= additive_expression");
                return parseEndSymbol(Token.LEQ) && parseAdditiveExpression();
            case Token.GEQ:
                dump("compare_expression' -> >= additive_expression");
                return parseEndSymbol(Token.GEQ) && parseAdditiveExpression();
            case Token.LTH:
                dump("compare_expression' -> < additive_expression");
                return parseEndSymbol(Token.LTH) && parseAdditiveExpression();
            case Token.GTH:
                dump("compare_expression' -> > additive_expression");
                return parseEndSymbol(Token.GTH) && parseAdditiveExpression();
            default:
                dump("compare_expression' -> ε");
                return true;
        }

    }

    /**
     * additive_expression -> multiplicative_expression additive_expression'
     */
    private boolean parseAdditiveExpression() {
        dump("additive_expression -> multiplicative_expression additive_expression'");
        return parseMultiplicativeExpression() && parseAdditiveExpression_();
    }

    /**
     * additive_expression' -> + multiplicative_expression additive_expression' .
     * additive_expression' -> - multiplicative_expression additive_expression' .
     * additive_expression' -> .
     */
    private boolean parseAdditiveExpression_() {
        switch (peek()) {
            case Token.ADD:
                dump("additive_expression' -> + multiplicative_expression additive_expression'");
                return parseEndSymbol(Token.ADD)
                        && parseMultiplicativeExpression()
                        && parseAdditiveExpression_();
            case Token.SUB:
                dump("additive_expression' -> - multiplicative_expression additive_expression'");
                return parseEndSymbol(Token.SUB)
                        && parseMultiplicativeExpression()
                        && parseAdditiveExpression_();
            default:
                dump("additive_expression' -> ε");
                return true;
        }
    }

    /**
     * multiplicative_expression -> prefix_expression multiplicative_expression'
     */
    private boolean parseMultiplicativeExpression() {
        dump("multiplicative_expression -> prefix_expression multiplicative_expression'");
        return parsePrefixExpression() && parseMultiplicativeExpression_();
    }

    /**
     * multiplicative_expression' -> * prefix_expression multiplicative_expression'
     * multiplicative_expression' -> / prefix_expression multiplicative_expression'
     * multiplicative_expression' -> % prefix_expression multiplicative_expression'
     * multiplicative_expression' -> .
     */
    private boolean parseMultiplicativeExpression_() {
        switch (peek()) {
            case Token.MUL:
                dump("multiplicative_expression' -> * prefix_expression multiplicative_expression'");
                return parseEndSymbol(Token.MUL) && parsePrefixExpression() && parseMultiplicativeExpression_();
            case Token.DIV:
                dump("multiplicative_expression' -> / prefix_expression multiplicative_expression'");
                return parseEndSymbol(Token.DIV) && parsePrefixExpression() && parseMultiplicativeExpression_();
            case Token.MOD:
                dump("multiplicative_expression' -> % prefix_expression multiplicative_expression'");
                return parseEndSymbol(Token.MOD) && parsePrefixExpression() && parseMultiplicativeExpression_();
            default:
                dump("multiplicative_expression' -> ε");
                return true;
        }
    }

    /**
     * prefix_expression -> + prefix_expression .
     * prefix_expression -> - prefix_expression .
     * prefix_expression -> ! prefix_expression .
     * prefix_expression -> postfix_expression .
     */
    private boolean parsePrefixExpression() {
        switch (peek()) {
            case Token.ADD:
                dump("prefix_expression -> + prefix_expression");
                return parseEndSymbol(Token.ADD) && parsePrefixExpression();
            case Token.SUB:
                dump("prefix_expression -> - prefix_expression");
                return parseEndSymbol(Token.SUB) && parsePrefixExpression();
            case Token.NOT:
                dump("prefix_expression -> ! prefix_expression");
                return parseEndSymbol(Token.NOT) && parsePrefixExpression();
            default:
                dump("prefix_expression -> postfix_expression");
                return parsePostfixExpression();
        }
    }

    /**
     * postfix_expression -> atom_expression postfix_expression' .
     */
    private boolean parsePostfixExpression() {
        dump("postfix_expression -> atom_expression postfix_expression'");
        return parseAtomExpression() && parsePostfixExpression_();
    }

    /**
     * postfix_expression' -> [ expression ] postfix_expression' .
     * postfix_expression' -> .
     * @return
     */
    private boolean parsePostfixExpression_() {
        if (peek() == Token.LBRACKET) {
            dump("postfix_expression' -> [ expression ] postfix_expression'");
            return parseEndSymbol(Token.LBRACKET)
                    && parseExpression()
                    && parseEndSymbol(Token.RBRACKET)
                    && parsePostfixExpression_();
        }
        dump("postfix_expression' -> ε");
        return true;

    }

    /**
     * atom_expression -> log_constant .
     * atom_expression -> int_constant .
     * atom_expression -> str_constant .
     * atom_expression -> ( expressions ) .
     * atom_expression -> identifier atom_expression' .
     * atom_expression -> { atom_expression''' .
     */
    private boolean parseAtomExpression() {
        switch (peek()) {
            case Token.LOG_CONST:
                dump("atom_expression -> log_constant");
                return parseEndSymbol(Token.LOG_CONST);
            case Token.INT_CONST:
                dump("atom_expression -> int_constant");
                return parseEndSymbol(Token.INT_CONST);
            case Token.STR_CONST:
                dump("atom_expression -> str_constant");
                return parseEndSymbol(Token.STR_CONST);
            case Token.LPARENT:
                dump("atom_expression -> ( expressions )");
                return parseEndSymbol(Token.LPARENT)
                        && parseExpressions()
                        && parseEndSymbol(Token.RPARENT);
            case Token.IDENTIFIER:
                dump("atom_expression -> identifier atom_expression'");
                return parseEndSymbol(Token.IDENTIFIER) && parseAtomExpression_();
            case Token.LBRACE:
                dump("atom_expression -> { atom_expression'''");
                return parseEndSymbol(Token.LBRACE) && parseAtomExpression___();
            default:
                Report.error(nextSym.position, "Invalid atom expression: " + this.nextSym.toString());
                return false;
        }
    }

    /**
     * atom_expression' -> ( expressions )
     * atom_expression' -> .
     */
    private boolean parseAtomExpression_() {
        if (peek() == Token.LPARENT) {
            dump("atom_expression' -> ( expressions )");
            return parseEndSymbol(Token.LPARENT)
                    && parseExpressions()
                    && parseEndSymbol(Token.RPARENT);
        }
        dump("atom_expression' -> ε");
        return true;
    }

    /**
     * atom_expression'' -> }
     * atom_expression'' -> else expression }
     */
    private boolean parseAtomExpression__() {
        switch (peek()) {
            case Token.RBRACE:
                dump("atom_expression'' -> } ");
                return parseEndSymbol(Token.RBRACE);
            case Token.KW_ELSE:
                dump("atom_expression'' -> else expression }");
                return parseEndSymbol(Token.KW_ELSE)
                        && parseExpression()
                        && parseEndSymbol(Token.RBRACE);
            default:
                Report.error(nextSym.position, "Invalid atom expression: " + this.nextSym.toString());
                return false;
        }
    }

    /**
     * atom_expression''' -> if expression then expression atom_expression'' .
     * atom_expression''' -> while expression : expression } .
     * atom_expression''' -> for identifier = expression , expression , expression : expression } .
     * atom_expression''' -> expression = expression } .
     */
    private boolean parseAtomExpression___() {
        switch (peek()) {
            case Token.KW_IF:
                dump("atom_expression''' -> if expression then expression atom_expression''");
                return parseEndSymbol(Token.KW_IF)
                        && parseExpression()
                        && parseEndSymbol(Token.KW_THEN)
                        && parseExpression()
                        && parseAtomExpression__();
            case Token.KW_WHILE:
                dump("atom_expression''' -> while expression : expression }");
                return parseEndSymbol(Token.KW_WHILE)
                        && parseExpression()
                        && parseEndSymbol(Token.COLON)
                        && parseExpression()
                        && parseEndSymbol(Token.RBRACE);
            case Token.KW_FOR:
                dump("atom_expression''' -> for identifier = expression , expression , expression : expression }");
                return parseEndSymbol(Token.KW_FOR)
                        && parseEndSymbol(Token.IDENTIFIER)
                        && parseEndSymbol(Token.ASSIGN)
                        && parseExpression()
                        && parseEndSymbol(Token.COMMA)
                        && parseExpression()
                        && parseEndSymbol(Token.COMMA)
                        && parseExpression()
                        && parseEndSymbol(Token.COLON)
                        && parseExpression()
                        && parseEndSymbol(Token.RBRACE);
            default:
                dump("atom_expression''' -> expression = expression }");
                return parseExpression()
                        && parseEndSymbol(Token.ASSIGN)
                        && parseExpression()
                        && parseEndSymbol(Token.RBRACE);

        }
    }

    /**
     * expressions -> expression expressions'
     */
    private boolean parseExpressions() {
        dump("expressions -> expression expressions'");
        return parseExpression() && parseExpressions_();
    }

    /**
     * expressions' -> , expression expressions'
     * expressions' -> .
     */
    private boolean parseExpressions_() {
        if (peek() == Token.COMMA) {
            dump("expressions' -> , expression expressions'");
            return parseEndSymbol(Token.COMMA)
                    && parseExpression()
                    && parseExpressions_();
        }
        dump("expressions' -> ε");
        return true;
    }




    /**
     *
     *
     */
    private boolean parseEndSymbol(int t) {
        if (peek() == t) {
            nextToken();
            return true;
        }
        Report.error(nextSym.position, "Invalid symbol: " + this.nextSym.toString());
        return false;
    }



    /**
	 * Izpise produkcijo v datoteko z vmesnimi rezultati.
	 * 
	 * @param production
	 *            Produkcija, ki naj bo izpisana.
	 */
	private void dump(String production) {
		if (!dump)
			return;
		if (Report.dumpFile() == null)
			return;
		Report.dumpFile().println(production);
	}

}
