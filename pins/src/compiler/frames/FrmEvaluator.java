package compiler.frames;

import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.SymbDesc;

import java.util.Stack;

public class FrmEvaluator implements Visitor {

    private int level = 0;

    private Stack<FrmFrame> stack = new Stack<>();


    @Override
    public void visit(AbsDefs acceptor) {
        for (int i = 0; i < acceptor.numDefs(); i++) {
            acceptor.def(i).accept(this);
        }
    }

    @Override
    public void visit(AbsExprs acceptor) {
        for (int i = 0; i < acceptor.numExprs(); i++) {
            acceptor.expr(i).accept(this);
        }
    }

    @Override
    public void visit(AbsBinExpr acceptor) {
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);
    }

    @Override
    public void visit(AbsUnExpr acceptor) {
        acceptor.expr.accept(this);
    }


    @Override
    public void visit(AbsFunDef acceptor) {
        // add new frame to stack
        stack.push(new FrmFrame(acceptor, level));

        for (int i = 0; i < acceptor.numPars(); i++) {
            acceptor.par(i).accept(this);
        }

        level++;
        acceptor.expr.accept(this);
        level--;

        FrmDesc.setFrame(acceptor, stack.pop());
    }

    @Override
    public void visit(AbsPar acceptor) {
        FrmDesc.setAccess(acceptor, new FrmParAccess(acceptor, stack.peek()));
    }

    @Override
    public void visit(AbsFunCall acceptor) {
        // set size of args to 4 (static link)
        int sizeArgs = 4;

        for (int i = 0; i < acceptor.numArgs(); i++) {
            acceptor.arg(i).accept(this);
            sizeArgs += SymbDesc.getType(acceptor.arg(i)).size();
        }

        // get max of args or results size
        sizeArgs = Math.max(sizeArgs, SymbDesc.getType(acceptor).size());

        // update sizeArgs of current function, if this function call needs the most space for arguments
        FrmFrame f = stack.peek();
        if (f.sizeArgs < sizeArgs) f.sizeArgs = sizeArgs;
    }

    @Override
    public void visit(AbsVarDef acceptor) {
        if (level == 0) {
            // set var as global
            FrmDesc.setAccess(acceptor, new FrmVarAccess(acceptor));
        } else {
            FrmFrame f = stack.peek();

            // set var as local
            FrmLocAccess la = new FrmLocAccess(acceptor, f);
            FrmDesc.setAccess(acceptor, la);

            // add var access to frame
            f.locVars.add(la);
        }
    }

    @Override
    public void visit(AbsIfThen accpetor) {
        accpetor.thenBody.accept(this);
    }

    @Override
    public void visit(AbsIfThenElse accpetor) {
        accpetor.thenBody.accept(this);
        accpetor.elseBody.accept(this);
    }

    @Override
    public void visit(AbsFor acceptor) {
        acceptor.lo.accept(this);
        acceptor.hi.accept(this);
        acceptor.step.accept(this);
        acceptor.count.accept(this);

        acceptor.body.accept(this);
    }

    @Override
    public void visit(AbsWhile acceptor) {
        acceptor.cond.accept(this);
        acceptor.body.accept(this);
    }

    @Override
    public void visit(AbsTenary acceptor) {
        acceptor.exprLog.accept(this);
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);
    }

    @Override
    public void visit(AbsWhere acceptor) {
        acceptor.expr.accept(this);
        acceptor.defs.accept(this);
    }

    //

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
    public void visit(AbsTypeDef acceptor) {

    }

    @Override
    public void visit(AbsTypeName acceptor) {

    }

    @Override
    public void visit(AbsVarName acceptor) {

    }
}
