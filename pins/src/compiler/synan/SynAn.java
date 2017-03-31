package compiler.synan;

import compiler.Position;
import compiler.Report;
import compiler.abstr.tree.*;
import compiler.lexan.*;

import java.util.Vector;

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

    private Position currentPosition() {
	    peek();
	    return nextSym.position;
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
	public AbsTree parse() {


        dump("source -> definitions");
        AbsTree abs = parseDefinitions();

        if(peek() != Token.EOF) {
            Report.error("expected EOF, found: " + nextSym);
        }

        return abs;

	}

    /**
     * definitions -> definition definitions' .
     * */
	private AbsTree parseDefinitions() {
        dump("definitions -> definition definitions'");

        Vector<AbsDef> defs = new Vector<>();
        defs.add(parseDefinition());

        Vector<AbsDef> defs1 = parseDefinitions_();
        if (defs1 != null) {
            defs.addAll(defs1);
        }

        return new AbsDefs(defs.get(0).position, defs);

	}

    /**
     * definition -> type_definition .
     * definition -> function_definition .
     * definition -> variable_definition .
     */
    private AbsDef parseDefinition() {
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
            default:
                Report.error(nextSym.position,"Invalid type or variable definition: " + nextSym.lexeme);
                return null;
        }
    }

    /**
     * definitions' -> ; definition definitions' .
     * definitions' -> .
     */
    private Vector<AbsDef> parseDefinitions_() {
        dump("definitions' -> ; definition definitions'");
        if (peek() == Token.SEMIC) {
            parseEndSymbol(Token.SEMIC);

            Vector<AbsDef> defs = new Vector<>();
            defs.add(parseDefinition());

            Vector<AbsDef> defs1 = parseDefinitions_();
            if (defs1 != null) {
                defs.addAll(defs1);
            }

            return defs;
        }
        dump("definitions' -> ε");
        return null;
    }

    /**
     * type_definition -> typ identifier : type
     */
    private AbsTypeDef parseTypeDefinition() {
        dump("type_definition -> typ identifier : type");
        Position p = nextSym.position;
        parseEndSymbol(Token.KW_TYP);

        String name = parseEndSymbol(Token.IDENTIFIER);

        parseEndSymbol(Token.COLON);

        AbsType t = parseType();
        return new AbsTypeDef(p, name, t);
    }

    /**
     * function_definition -> fun identifier ( parameters ) : type = expression .
     */
    private AbsFunDef parseFunctionDefinition() {
        dump("function_definition -> fun identifier ( parameters ) : type = expression");
        Position p = currentPosition();
        parseEndSymbol(Token.KW_FUN);

        String name = parseEndSymbol(Token.IDENTIFIER);

        parseEndSymbol(Token.LPARENT);

        Vector<AbsPar> pars = parseParameters();

        parseEndSymbol(Token.RPARENT);
        parseEndSymbol(Token.COLON);

        AbsType type = parseType();

        parseEndSymbol(Token.ASSIGN);

        AbsExpr expr = parseExpression();

        return new AbsFunDef(p, name, pars, type, expr);
    }

    /**
     * variable_definition -> var identifier : type
     */
    private AbsVarDef parseVariableDefinition() {
        dump("variable_definition -> var identifier : type");
        Position p = currentPosition();
        parseEndSymbol(Token.KW_VAR);

        String name = parseEndSymbol(Token.IDENTIFIER);

        parseEndSymbol(Token.COLON);

        AbsType type = parseType();
        return new AbsVarDef(p, name, type);
    }

    /**
     * type -> identifier .
     * type -> logical .
     * type -> integer .
     * type -> string .
     * type -> arr [ int_const ] type .
     *
     */
    private AbsType parseType() {
        String name;
        Position p = currentPosition();
        switch (peek()) {
            case Token.IDENTIFIER:
                dump("type -> identifier");
                name = parseEndSymbol(Token.IDENTIFIER);
                return new AbsTypeName(p, name);
            case Token.LOGICAL:
                dump("type -> logical");
                name = parseEndSymbol(Token.LOGICAL);
                return new AbsTypeName(p, name);
            case Token.INTEGER:
                dump("type -> integer");
                name = parseEndSymbol(Token.INTEGER);
                return new AbsTypeName(p, name);
            case Token.STRING:
                dump("type -> string");
                name = parseEndSymbol(Token.STRING);
                return new AbsTypeName(p, name);
            case Token.KW_ARR:
                dump("type -> arr [ int_const ] type");
                parseEndSymbol(Token.KW_ARR);
                parseEndSymbol(Token.LBRACKET);

                int tLen = Integer.parseInt(parseEndSymbol(Token.INT_CONST));

                parseEndSymbol(Token.RBRACKET);
                AbsType typeTree = parseType();
                return new AbsArrType(p, tLen, typeTree);
            default:
                Report.error(nextSym.position, "Invalid type name: " + this.nextSym.toString());
                return null;
        }

    }

    /**
     * parameters -> parameter parameters' .
     */
    private Vector<AbsPar> parseParameters() {
        dump("parameters -> parameter parameters'");

        Vector<AbsPar> pars = new Vector<>();
        pars.add(parseParameter());

        Vector<AbsPar> params = parseParameters_();
        if (params != null) {
            pars.addAll(params);
        }
        return pars;
    }

    /**
     * parameter -> identifier : type .
     */
    private AbsPar parseParameter() {
        dump("parameter -> identifier : type");
        Position p = currentPosition();
        String name = parseEndSymbol(Token.IDENTIFIER);

        parseEndSymbol(Token.COLON);

        AbsType type = parseType();
        return  new AbsPar(p, name, type);
    }

    /**
     * parameters' -> , parameter parameters' .
     * parameters' -> .
     */
    private Vector<AbsPar> parseParameters_() {
        if (peek() == Token.COMMA) {
            parseEndSymbol(Token.COMMA);
            Vector<AbsPar> pars = new Vector<>();
            pars.add(parseParameter());

            Vector<AbsPar> params = parseParameters_();
            if (params != null) {
                pars.addAll(params);
            }
            return pars;
        }

        dump("parameters' -> ε");
        return null;
    }

    /**
     * expression -> logical_ior_expression expression'
     */
    private AbsExpr parseExpression() {

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
        return null;
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
    private String parseEndSymbol(int t) {
        if (peek() == t) {
            String l = nextSym.lexeme;

            nextToken();

            return l;
        }
        Report.error(nextSym.position, "Invalid symbol: " + this.nextSym.toString());
        return "";
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
