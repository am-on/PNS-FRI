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
	private AbsDefs parseDefinitions() {
        dump("definitions -> definition definitions'");

        Vector<AbsDef> defs = new Vector<>();
        defs.add(parseDefinition());

        Vector<AbsDef> defs1 = parseDefinitions_();
        if (defs1 != null) {
            defs.addAll(defs1);
        }
        Position p = new Position(defs.get(0).position, defs.lastElement().position);
        return new AbsDefs(p, defs);

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
        Position p = currentPosition();
        parseEndSymbol(Token.KW_TYP);

        String name = parseEndSymbol(Token.IDENTIFIER);

        parseEndSymbol(Token.COLON);

        AbsType t = parseType();
        p = new Position(p, t.position);
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
        p = new Position(p, expr.position);
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

        p = new Position(p, type.position);
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
                parseEndSymbol(Token.LOGICAL);
                return new AbsAtomType(p, AbsAtomType.LOG);
            case Token.INTEGER:
                dump("type -> integer");
                parseEndSymbol(Token.INTEGER);
                return new AbsAtomType(p, AbsAtomType.INT);
            case Token.STRING:
                dump("type -> string");
                parseEndSymbol(Token.STRING);
                return new AbsAtomType(p, AbsAtomType.STR);
            case Token.KW_ARR:
                dump("type -> arr [ int_const ] type");
                parseEndSymbol(Token.KW_ARR);
                parseEndSymbol(Token.LBRACKET);

                int tLen = Integer.parseInt(parseEndSymbol(Token.INT_CONST));

                parseEndSymbol(Token.RBRACKET);
                AbsType typeTree = parseType();
                p = new Position(p, typeTree.position);
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
        p = new Position(p, type.position);
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
        dump("expression -> logical_ior_expression expression'");
        AbsExpr e1 = parseLogicalIorExpression();
        return parseExpression_(e1);
    }

    /**
     * expression' -> { WHERE definitions }
     * expression' -> .
     */
    private AbsExpr parseExpression_(AbsExpr e) {
        if (peek() == Token.LBRACE) {
            dump("expression' -> { WHERE definitions }");
            parseEndSymbol(Token.LBRACE);
            parseEndSymbol(Token.KW_WHERE);

            AbsDefs defs = parseDefinitions();
            Position p = new Position(e.position, currentPosition());
            parseEndSymbol(Token.RBRACE);

            return new AbsWhere(e.position, e, defs);
        }
        dump("expression' -> ε");
        return e;
    }

    /**
     * logical_ior_expression -> logical_and_expression logical_ior_expression'     *
     */
    private AbsExpr parseLogicalIorExpression() {
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression' ");
        AbsExpr aExpr = parseLogicalAndExpression();
        return parseLogicalIorExpression_(aExpr);
    }

    /**
     * logical_ior_expression' -> | logical_and_expression logical_ior_expression' .
     * logical_ior_expression' -> .
     * @return
     */
    private AbsExpr parseLogicalIorExpression_(AbsExpr e) {
        if (peek() == Token.IOR) {
            dump("logical_ior_expression' -> | logical_and_expression logical_ior_expression'");
            Position p = currentPosition();

            parseEndSymbol(Token.IOR);

            AbsExpr a = parseLogicalAndExpression();
            p = new Position(p, currentPosition());
            AbsBinExpr join = new AbsBinExpr(p, AbsBinExpr.IOR, e, a);

            return parseLogicalIorExpression_(join);
        }
        dump("logical_ior_expression' -> ε");
        return e;
    }

    /**
     * logical_and_expression -> compare_expression logical_and_expression'
     */
    private AbsExpr parseLogicalAndExpression() {
        dump("logical_and_expression -> compare_expression logical_and_expression'");
        AbsExpr cExpr = parseCompareExpression();
        return parseLogicalAndExpression_(cExpr);
    }

    /**
     * logical_and_expression' -> & compare_expression logical_and_expression'
     * logical_and_expression' -> .
     */
    private AbsExpr parseLogicalAndExpression_(AbsExpr e) {
        if (peek() == Token.AND) {
            dump("logical_and_expression' -> & compare_expression logical_and_expression'");

            Position p = currentPosition();
            parseEndSymbol(Token.AND);

            AbsExpr e2 = parseCompareExpression();
            p = new Position(p, currentPosition());
            AbsBinExpr join = new AbsBinExpr(p, AbsBinExpr.AND, e, e2);
            return parseLogicalAndExpression_(join);
        }
        dump("logical_and_expression' -> ε");
        return e;
    }

    /**
     * compare_expression -> additive_expression compare_expression'
     */
    private AbsExpr parseCompareExpression() {
        dump("compare_expression -> additive_expression compare_expression'");
        AbsExpr e = parseAdditiveExpression();
        return parseCompareExpression_(e);
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
    private AbsExpr parseCompareExpression_(AbsExpr e) {
        Position p = e.position;
        AbsExpr e2;
        switch (peek()) {
            case Token.EQU:
                dump("compare_expression' -> == additive_expression");
                parseEndSymbol(Token.EQU);

				e2 = parseAdditiveExpression();
                p = new Position(p, e2.position);
				return new AbsBinExpr(p, AbsBinExpr.EQU, e, e2);
            case Token.NEQ:
                dump("compare_expression' -> != additive_expression");
                parseEndSymbol(Token.NEQ);

				e2 = parseAdditiveExpression();
                p = new Position(p, e2.position);
				return new AbsBinExpr(p, AbsBinExpr.NEQ, e, e2);
            case Token.LEQ:
                dump("compare_expression' -> <= additive_expression");
                parseEndSymbol(Token.LEQ);

				e2 = parseAdditiveExpression();
                p = new Position(p, e2.position);
				return new AbsBinExpr(p, AbsBinExpr.LEQ, e, e2);
            case Token.GEQ:
                dump("compare_expression' -> >= additive_expression");
                parseEndSymbol(Token.GEQ);

				e2 = parseAdditiveExpression();
                p = new Position(p, e2.position);
				return new AbsBinExpr(p, AbsBinExpr.GEQ, e, e2);
            case Token.LTH:
                dump("compare_expression' -> < additive_expression");
                parseEndSymbol(Token.LTH);

				e2 = parseAdditiveExpression();
                p = new Position(p, e2.position);
				return new AbsBinExpr(p, AbsBinExpr.LTH, e, e2);
            case Token.GTH:
                dump("compare_expression' -> > additive_expression");
                parseEndSymbol(Token.GTH);

				e2 = parseAdditiveExpression();
                p = new Position(p, e2.position);
				return new AbsBinExpr(p, AbsBinExpr.GTH, e, e2);
            default:
                dump("compare_expression' -> ε");
                return e;
        }

    }

    /**
     * additive_expression -> multiplicative_expression additive_expression'
     */
    private AbsExpr parseAdditiveExpression() {
        dump("additive_expression -> multiplicative_expression additive_expression'");
        AbsExpr e = parseMultiplicativeExpression();
        return parseAdditiveExpression_(e);
    }

    /**
     * additive_expression' -> + multiplicative_expression additive_expression' .
     * additive_expression' -> - multiplicative_expression additive_expression' .
     * additive_expression' -> .
     */
    private AbsExpr parseAdditiveExpression_(AbsExpr e) {
        Position p = e.position;
        AbsExpr e2;
        AbsExpr join;
        switch (peek()) {
            case Token.ADD:
                dump("additive_expression' -> + multiplicative_expression additive_expression'");
                parseEndSymbol(Token.ADD);

                e2 = parseMultiplicativeExpression();
                p = new Position(p, e2.position);
                join = new AbsBinExpr(p, AbsBinExpr.ADD, e, e2);
                return parseAdditiveExpression_(join);
            case Token.SUB:
                dump("additive_expression' -> - multiplicative_expression additive_expression'");
                parseEndSymbol(Token.SUB);

                e2 = parseMultiplicativeExpression();
                p = new Position(p, e2.position);
                join = new AbsBinExpr(p, AbsBinExpr.SUB, e, e2);
                return parseAdditiveExpression_(join);

            default:
                dump("additive_expression' -> ε");
                return e;
        }
    }

    /**
     * multiplicative_expression -> prefix_expression multiplicative_expression'
     */
    private AbsExpr parseMultiplicativeExpression() {
        dump("multiplicative_expression -> prefix_expression multiplicative_expression'");
        AbsExpr e = parsePrefixExpression();
        return parseMultiplicativeExpression_(e);
    }

    /**
     * multiplicative_expression' -> * prefix_expression multiplicative_expression'
     * multiplicative_expression' -> / prefix_expression multiplicative_expression'
     * multiplicative_expression' -> % prefix_expression multiplicative_expression'
     * multiplicative_expression' -> .
     */
    private AbsExpr parseMultiplicativeExpression_(AbsExpr e) {
        Position p = e.position;
        AbsExpr e2;
        AbsExpr join;
        switch (peek()) {
            case Token.MUL:
                dump("multiplicative_expression' -> * prefix_expression multiplicative_expression'");
                parseEndSymbol(Token.MUL);
                e2 = parsePrefixExpression();
                p = new Position(p, e2.position);
                join = new AbsBinExpr(p, AbsBinExpr.MUL, e, e2);
                return parseMultiplicativeExpression_(join);
            case Token.DIV:
                dump("multiplicative_expression' -> / prefix_expression multiplicative_expression'");
                parseEndSymbol(Token.DIV);
                e2 = parsePrefixExpression();
                p = new Position(p, e2.position);
                join = new AbsBinExpr(p, AbsBinExpr.DIV, e, e2);
                return parseMultiplicativeExpression_(join);
            case Token.MOD:
                dump("multiplicative_expression' -> % prefix_expression multiplicative_expression'");
                parseEndSymbol(Token.MOD);
                e2 = parsePrefixExpression();
                p = new Position(p, e2.position);
                join = new AbsBinExpr(p, AbsBinExpr.MOD, e, e2);
                return parseMultiplicativeExpression_(join);
            default:
                dump("multiplicative_expression' -> ε");
                return e;
        }
    }

    /**
     * prefix_expression -> + prefix_expression .
     * prefix_expression -> - prefix_expression .
     * prefix_expression -> ! prefix_expression .
     * prefix_expression -> postfix_expression .
     */
    private AbsExpr parsePrefixExpression() {
        Position p = currentPosition();
        AbsExpr e2;
        switch (peek()) {
            case Token.ADD:
                dump("prefix_expression -> + prefix_expression");
                parseEndSymbol(Token.ADD);
                e2 = parsePrefixExpression();
                p = new Position(p, e2.position);
                return new AbsUnExpr(p, AbsUnExpr.ADD, e2);

            case Token.SUB:
                dump("prefix_expression -> - prefix_expression");
                parseEndSymbol(Token.SUB);
                e2 = parsePrefixExpression();
                p = new Position(p, e2.position);
                return new AbsUnExpr(p, AbsUnExpr.SUB, e2);
            case Token.NOT:
                dump("prefix_expression -> ! prefix_expression");
                parseEndSymbol(Token.NOT);
                e2 = parsePrefixExpression();
                p = new Position(p, e2.position);
                return new AbsUnExpr(p, AbsUnExpr.NOT, e2);
            default:
                dump("prefix_expression -> postfix_expression");
                return parsePostfixExpression();
        }
    }

    /**
     * postfix_expression -> atom_expression postfix_expression' .
     */
    private AbsExpr parsePostfixExpression() {
        dump("postfix_expression -> atom_expression postfix_expression'");
        AbsExpr e = parseAtomExpression();
        return parsePostfixExpression_(e);
    }

    /**
     * postfix_expression' -> [ expression ] postfix_expression' .
     * postfix_expression' -> .
     * @return
     */
    private AbsExpr parsePostfixExpression_(AbsExpr e) {
        Position p = currentPosition();

        if (peek() == Token.LBRACKET) {
            dump("postfix_expression' -> [ expression ] postfix_expression'");
            parseEndSymbol(Token.LBRACKET);

            AbsExpr e2 = parseExpression();
            p = new Position(p, e2.position);
            AbsBinExpr join = new AbsBinExpr(p, AbsBinExpr.ARR, e, e2);

            parseEndSymbol(Token.RBRACKET);

            return  parsePostfixExpression_(join);
        }
        dump("postfix_expression' -> ε");
        return e;

    }

    /**
     * atom_expression -> log_constant .
     * atom_expression -> int_constant .
     * atom_expression -> str_constant .
     * atom_expression -> ( expressions ) .
     * atom_expression -> identifier atom_expression' .
     * atom_expression -> { atom_expression''' .
     */
    private AbsExpr parseAtomExpression() {
        Position p = currentPosition();
        String name;
        AbsExprs e;
        Vector<AbsExpr> exprs;
        switch (peek()) {
            case Token.LOG_CONST:
                dump("atom_expression -> log_constant");
                name = parseEndSymbol(Token.LOG_CONST);
                return new AbsAtomConst(p, AbsAtomConst.LOG, name);
            case Token.INT_CONST:
                dump("atom_expression -> int_constant");
                name = parseEndSymbol(Token.INT_CONST);
                return new AbsAtomConst(p, AbsAtomConst.INT, name);
            case Token.STR_CONST:
                dump("atom_expression -> str_constant");
                name = parseEndSymbol(Token.STR_CONST);
                return new AbsAtomConst(p, AbsAtomConst.STR, name);
            case Token.LPARENT:
                dump("atom_expression -> ( expressions )");
                parseEndSymbol(Token.LPARENT);

                exprs = parseExpressions();
                p = new Position(p, currentPosition());
                e = new AbsExprs(p, exprs);

                parseEndSymbol(Token.RPARENT);

                return e;
            case Token.IDENTIFIER:
                dump("atom_expression -> identifier atom_expression'");
                p = currentPosition();
                name = parseEndSymbol(Token.IDENTIFIER);
                exprs = new Vector<>();

                Vector<AbsExpr> e2 = parseAtomExpression_();
                if (e2 != null) {
                    exprs.addAll(e2);
                    p = new Position(p, exprs.lastElement().position);
                    return new AbsFunCall(p, name, exprs);
                }

                return new AbsVarName(p, name);
            case Token.LBRACE:
                dump("atom_expression -> { atom_expression'''");
                p = currentPosition();
                parseEndSymbol(Token.LBRACE);
                return parseAtomExpression___(p);
            default:
                Report.error(nextSym.position, "Invalid atom expression: " + this.nextSym.toString());
                return null;
        }
    }

    /**
     * atom_expression' -> ( expressions )
     * atom_expression' -> .
     */
    private Vector<AbsExpr> parseAtomExpression_() {
        if (peek() == Token.LPARENT) {
            dump("atom_expression' -> ( expressions )");
            parseEndSymbol(Token.LPARENT);
            Vector<AbsExpr> e = parseExpressions();
            parseEndSymbol(Token.RPARENT);

            return e;
        }
        dump("atom_expression' -> ε");
        return null;
    }

    /**
     * atom_expression'' -> }
     * atom_expression'' -> else expression }
     */
    private AbsExpr parseAtomExpression__(Position p, AbsExpr e1, AbsExpr e2) {
        switch (peek()) {
            case Token.RBRACE:
                dump("atom_expression'' -> } ");

                p = new Position(p, currentPosition());

                parseEndSymbol(Token.RBRACE);

                return new AbsIfThen(p, e1, e2);
            case Token.KW_ELSE:
                dump("atom_expression'' -> else expression }");
                parseEndSymbol(Token.KW_ELSE);

                AbsExpr e3 = parseExpression();
                p = new Position(p, currentPosition());

                parseEndSymbol(Token.RBRACE);

                return new AbsIfThenElse(p, e1, e2, e3);
            default:
                Report.error(nextSym.position, "Invalid atom expression: " + this.nextSym.toString());
                return null;
        }
    }

    /**
     * atom_expression''' -> if expression then expression atom_expression'' .
     * atom_expression''' -> while expression : expression } .
     * atom_expression''' -> for identifier = expression , expression , expression : expression } .
     * atom_expression''' -> expression = expression } .
     */
    private AbsExpr parseAtomExpression___(Position p) {
        AbsExpr e1;
        AbsExpr e2;
        AbsExpr e3;
        AbsExpr e4;
        switch (peek()) {
            case Token.KW_IF:
                dump("atom_expression''' -> if expression then expression atom_expression''");
                parseEndSymbol(Token.KW_IF);

                e1 = parseExpression();

                parseEndSymbol(Token.KW_THEN);

                e2 = parseExpression();

                p = new Position(p, e2.position);
                return  parseAtomExpression__(p, e1, e2);

            case Token.KW_WHILE:
                dump("atom_expression''' -> while expression : expression }");
                parseEndSymbol(Token.KW_WHILE);

                e1 = parseExpression();

                parseEndSymbol(Token.COLON);

                e2 = parseExpression();
                p = new Position(p, currentPosition());

                parseEndSymbol(Token.RBRACE);


                return new AbsWhile(p, e1, e2);

            case Token.KW_FOR:
                dump("atom_expression''' -> for identifier = expression , expression , expression : expression }");
                parseEndSymbol(Token.KW_FOR);

                AbsVarName varName = new AbsVarName(currentPosition(), parseEndSymbol(Token.IDENTIFIER));

				parseEndSymbol(Token.ASSIGN);

				e1 = parseExpression();

				parseEndSymbol(Token.COMMA);

				e2 = parseExpression();

				parseEndSymbol(Token.COMMA);

				e3 = parseExpression();

				parseEndSymbol(Token.COLON);

				e4 = parseExpression();

                p = new Position(p, currentPosition());

				parseEndSymbol(Token.RBRACE);

				return new AbsFor(p, varName, e1, e2, e3, e4);
            default:
                dump("atom_expression''' -> expression = expression }");

                e1 = parseExpression();

                parseEndSymbol(Token.ASSIGN);

                e2 = parseExpression();
                p = new Position(p, currentPosition());

                parseEndSymbol(Token.RBRACE);

                return new AbsBinExpr(p, AbsBinExpr.ASSIGN, e1, e2);
        }
    }

    /**
     * expressions -> expression expressions'
     */
    private Vector<AbsExpr> parseExpressions() {
        dump("expressions -> expression expressions'");
        Vector<AbsExpr> exprs = new Vector<>();
        exprs.add(parseExpression());

        Vector<AbsExpr> e2 = parseExpressions_();
        if (e2 != null)
            exprs.addAll(e2);

        return exprs;
    }

    /**
     * expressions' -> , expression expressions'
     * expressions' -> .
     */
    private Vector<AbsExpr> parseExpressions_() {
        if (peek() == Token.COMMA) {
            dump("expressions' -> , expression expressions'");
            parseEndSymbol(Token.COMMA);

            Vector<AbsExpr> exprs = new Vector<>();
            exprs.add(parseExpression());

            Vector<AbsExpr> e2 = parseExpressions_();
            if (e2 != null)
                exprs.addAll(e2);

            return exprs;
        }
        dump("expressions' -> ε");
        return null;
    }

    /**
     * parse given end symbol
     */
    private String parseEndSymbol(int t) {
        if (peek() == t) {
            String l = nextSym.lexeme;

            nextToken();

            return l;
        }
        Report.error(nextSym.position, "Invalid symbol: " + this.nextSym.toString());
        return null;
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
