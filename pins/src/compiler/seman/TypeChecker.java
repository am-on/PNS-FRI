package compiler.seman;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.type.*;

/**
 * Preverjanje tipov.
 * 
 * @author sliva
 */
public class TypeChecker implements Visitor {
    @Override
    public void visit(AbsArrType acceptor) {

    }

    @Override
    public void visit(AbsAtomConst acceptor) {

    }

    @Override
    public void visit(AbsAtomType acceptor) {

    }

    @Override
    public void visit(AbsBinExpr acceptor) {

    }

    @Override
    public void visit(AbsDefs acceptor) {
        acceptor.accept(this);
    }

    @Override
    public void visit(AbsExprs acceptor) {

    }

    @Override
    public void visit(AbsFor acceptor) {

    }

    @Override
    public void visit(AbsFunCall acceptor) {

    }

    @Override
    public void visit(AbsFunDef acceptor) {

    }

    @Override
    public void visit(AbsIfThen accpetor) {

    }

    @Override
    public void visit(AbsIfThenElse accpetor) {

    }

    @Override
    public void visit(AbsPar acceptor) {

    }

    @Override
    public void visit(AbsTypeDef acceptor) {

    }

    @Override
    public void visit(AbsTypeName acceptor) {

    }

    @Override
    public void visit(AbsUnExpr acceptor) {

    }

    @Override
    public void visit(AbsVarDef acceptor) {

    }

    @Override
    public void visit(AbsVarName acceptor) {

    }

    @Override
    public void visit(AbsWhere acceptor) {

    }

    @Override
    public void visit(AbsWhile acceptor) {

    }
}

