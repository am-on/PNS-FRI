package compiler.imcode;

import java.util.*;

import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.frames.*;
import compiler.seman.SymbDesc;

public class ImcCodeGen implements Visitor {

    public LinkedList<ImcChunk> chunks;
    private Stack<FrmFrame> frames = new Stack<>();

    HashMap<AbsTree, ImcCode> imPart = new HashMap<>();

    public ImcCodeGen() {
        chunks = new LinkedList<ImcChunk>();
    }

    private void addExprStmts(ImcSEQ seq, ImcCode expr) {
        if (expr instanceof  ImcStmt) {
            seq.stmts.add((ImcStmt) expr);
        } else {
            seq.stmts.add(new ImcEXP((ImcExpr) expr));
        }
    }

    private ImcExpr getStaticLink(FrmFrame current, FrmFrame called, ImcExpr staticLink) {
        int diff = current.level - called.level;
        for (int i = 0; i < diff; i++) {
            staticLink = new ImcMEM(staticLink);
        }

        return staticLink;
    }

    @Override
    public void visit(AbsDefs acceptor) {
        for (int i = 0; i < acceptor.numDefs(); i++) {
            acceptor.def(i).accept(this);
        }
    }

    @Override
    public void visit(AbsAtomConst acceptor) {
        ImcCode imc = null;
        switch (acceptor.type) {
            case AbsAtomConst.INT:
                int value = Integer.parseInt(acceptor.value);
                imc = new ImcCONST(value);
                break;
            case AbsAtomConst.LOG:
                if (acceptor.value.equals("true")) {
                    imc = new ImcCONST(1);
                }
                else {
                    imc = new ImcCONST(0);
                }
                break;
            case AbsAtomConst.STR:
                FrmLabel strLabel = FrmLabel.newLabel();
                chunks.add(new ImcDataChunk(strLabel, 4));
                imc = new ImcNAME(strLabel);
                break;
        }
        imPart.put(acceptor, imc);
    }

    @Override
    public void visit(AbsBinExpr acceptor) {
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);

        ImcExpr expr1 = (ImcExpr) imPart.get(acceptor.expr1);
        ImcExpr expr2 = (ImcExpr) imPart.get(acceptor.expr2);


        switch (acceptor.oper) {
            case AbsBinExpr.IOR:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.OR, expr1, expr2));
                break;
            case AbsBinExpr.AND:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.AND, expr1, expr2));
                break;
            case AbsBinExpr.EQU:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.EQU, expr1, expr2));
                break;
            case AbsBinExpr.NEQ:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.NEQ, expr1, expr2));
                break;
            case AbsBinExpr.LEQ:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.LEQ, expr1, expr2));
                break;
            case AbsBinExpr.GEQ:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.GEQ, expr1, expr2));
                break;
            case AbsBinExpr.LTH:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.LTH, expr1, expr2));
                break;
            case AbsBinExpr.GTH:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.GTH, expr1, expr2));
                break;
            case AbsBinExpr.ADD:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.ADD, expr1, expr2));
                break;
            case AbsBinExpr.SUB:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.SUB, expr1, expr2));
                break;
            case AbsBinExpr.MUL:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.MUL, expr1, expr2));
                break;
            case AbsBinExpr.DIV:
                imPart.put(acceptor, new ImcBINOP(ImcBINOP.DIV, expr1, expr2));
                break;

            case AbsBinExpr.MOD:
                // create temp vars
                ImcTEMP x = new ImcTEMP(new FrmTemp());
                ImcTEMP y = new ImcTEMP(new FrmTemp());

                ImcSEQ modSeq = new ImcSEQ();

                // move exprs to temp vars
                modSeq.stmts.add(new ImcMOVE(x, expr1));
                modSeq.stmts.add(new ImcMOVE(y, expr2));

                // calculate mod: x mod y = x - (y * (x div y))
                ImcBINOP div = new ImcBINOP(ImcBINOP.DIV, x, y);
                ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, y, div);
                ImcBINOP result = new ImcBINOP(ImcBINOP.SUB, x, mul);

                imPart.put(acceptor, result);

                break;

            case AbsBinExpr.ARR:
                // TODO
            case AbsBinExpr.ASSIGN:
                // x = expression
                // store expression to x
                ImcMOVE store = new ImcMOVE(expr1, expr2);

                imPart.put(acceptor, store);
        }

    }

    @Override
    public void visit(AbsExprs acceptor) {

        for (int i = 0; i < acceptor.numExprs(); i++) {
            acceptor.expr(i).accept(this);
        }

        if (acceptor.numExprs() == 1) {
            ImcCode expr = imPart.get(acceptor.expr(0));
            imPart.put(acceptor, expr);
        } else {
            ImcSEQ exprSeq = new ImcSEQ();

            for (int i = 0; i < acceptor.numExprs()-1; i++) {
                addExprStmts(exprSeq, imPart.get(acceptor.expr(i)));
            }

            ImcCode lastExpr = imPart.get(acceptor.expr(acceptor.numExprs()-1));
            if (lastExpr instanceof  ImcExpr) {
                ImcESEQ eseq = new ImcESEQ(exprSeq, (ImcExpr) lastExpr);
                imPart.put(acceptor, eseq);
            } else {
                addExprStmts(exprSeq, lastExpr);
                imPart.put(acceptor, exprSeq);
            }
        }
    }

    @Override
    public void visit(AbsFor acceptor) {
        acceptor.count.accept(this);
        acceptor.lo.accept(this);
        acceptor.hi.accept(this);
        acceptor.step.accept(this);
        acceptor.body.accept(this);

        ImcSEQ seq = new ImcSEQ();

        FrmLabel condLabel = FrmLabel.newLabel();
        FrmLabel bodyLabel = FrmLabel.newLabel();
        FrmLabel endLabel = FrmLabel.newLabel();

        ImcExpr count = (ImcExpr) imPart.get(acceptor.count);
        ImcExpr lo = (ImcExpr) imPart.get(acceptor.lo);
        ImcExpr hi = (ImcExpr) imPart.get(acceptor.hi);
        ImcExpr step = (ImcExpr) imPart.get(acceptor.step);

        // set counter: i = lo
        seq.stmts.add(new ImcMOVE(count, lo));

        // mark condition place
        seq.stmts.add(new ImcLABEL(condLabel));

        // create condition: i < hi
        ImcBINOP cond = new ImcBINOP(ImcBINOP.LTH, count, hi);

        // set true and false jumps for condition
        seq.stmts.add(new ImcCJUMP(cond, bodyLabel, endLabel));

        // mark body place
        seq.stmts.add(new ImcLABEL(bodyLabel));

        // execute body
        ImcCode expr = imPart.get(acceptor.body);
        addExprStmts(seq, expr);

        // increase counter: i += step
        seq.stmts.add(new ImcMOVE(count, new ImcBINOP(ImcBINOP.ADD, count, step)));

        // jump to condition
        seq.stmts.add(new ImcJUMP(condLabel));

        // mark loop exit
        seq.stmts.add(new ImcLABEL(endLabel));

        imPart.put(acceptor, seq);
    }

    @Override
    public void visit(AbsFunCall acceptor) {
        for (int i = 0; i < acceptor.numArgs(); i++) {
            acceptor.arg(i).accept(this);
        }

        ImcExpr fp = new ImcTEMP(frames.peek().FP);
        FrmFrame calledFunFrame = FrmDesc.getFrame(SymbDesc.getNameDef(acceptor));

        // create function call
        ImcCALL functionCall = new ImcCALL(calledFunFrame.label);

        // add static link
        fp = getStaticLink(frames.peek(), calledFunFrame, fp);
        functionCall.args.add(fp);

        // add other args
        for (int i = 0; i < acceptor.numArgs(); i++) {
            ImcExpr arg = (ImcExpr) imPart.get(acceptor.arg(i));
            functionCall.args.add(arg);
        }

        imPart.put(acceptor, functionCall);

    }

    @Override
    public void visit(AbsFunDef acceptor) {
        // add frame of current function
        frames.push(FrmDesc.getFrame(acceptor));

        acceptor.expr.accept(this);

        ImcExpr expr = (ImcExpr) imPart.get(acceptor.expr);

        // get RV value
        ImcTEMP tempRV = new ImcTEMP(frames.peek().RV);

        chunks.add(new ImcCodeChunk(frames.peek(), new ImcMOVE(tempRV, expr)));

        frames.pop();
    }

    @Override
    public void visit(AbsIfThen accpetor) {
        accpetor.cond.accept(this);
        accpetor.thenBody.accept(this);

        ImcSEQ seq = new ImcSEQ();

        FrmLabel thenLabel = FrmLabel.newLabel();
        FrmLabel endLabel = FrmLabel.newLabel();

        // add conditional jump
        ImcExpr cond = (ImcExpr) imPart.get(accpetor.cond);
        seq.stmts.add(new ImcCJUMP(cond, thenLabel, endLabel));

        // mark true location
        seq.stmts.add(new ImcLABEL(thenLabel));

        // add body expression
        ImcCode body = imPart.get(accpetor.thenBody);
        addExprStmts(seq, body);

        // mark end location
        seq.stmts.add(new ImcLABEL(endLabel));

        imPart.put(accpetor, seq);
    }

    @Override
    public void visit(AbsIfThenElse accpetor) {
        accpetor.cond.accept(this);
        accpetor.thenBody.accept(this);
        accpetor.elseBody.accept(this);

        ImcSEQ seq = new ImcSEQ();

        FrmLabel thenLabel = FrmLabel.newLabel();
        FrmLabel elseLabel = FrmLabel.newLabel();
        FrmLabel endLabel = FrmLabel.newLabel();

        // add conditional jump
        ImcExpr cond = (ImcExpr) imPart.get(accpetor.cond);
        seq.stmts.add(new ImcCJUMP(cond, thenLabel, elseLabel));

        // mark true location
        seq.stmts.add(new ImcLABEL(thenLabel));

        // add body expression
        ImcCode body = imPart.get(accpetor.thenBody);
        addExprStmts(seq, body);

        // skip else part if body was executed
        seq.stmts.add(new ImcJUMP(endLabel));

        // mark false location
        seq.stmts.add(new ImcLABEL(elseLabel));

        // add else expression
        ImcCode elseBody = imPart.get(accpetor.elseBody);
        addExprStmts(seq, elseBody);

        // mark end location
        seq.stmts.add(new ImcLABEL(endLabel));

        imPart.put(accpetor, seq);


    }

    @Override
    public void visit(AbsVarDef acceptor) {
        // add global var
        if (frames.empty()) {
            int size = SymbDesc.getType(acceptor).size();
            FrmLabel label = ((FrmVarAccess) FrmDesc.getAccess(acceptor)).label;
            chunks.add(new ImcDataChunk(label, size));
        }
    }

    @Override
    public void visit(AbsVarName acceptor) {
        FrmAccess access = FrmDesc.getAccess(SymbDesc.getNameDef(acceptor));
        ImcMEM mem;

        // global var access
        if (access instanceof FrmVarAccess) {
            // get var label
            FrmLabel label = ((FrmVarAccess) access).label;
            mem = new ImcMEM(new ImcNAME(label));
        }
        // local or parameter var access
        else {
            ImcExpr fp = new ImcTEMP(frames.peek().FP);
            ImcCONST offset = null;
            FrmFrame frame = null;

            if (access instanceof FrmParAccess) {
                FrmParAccess parAcc = (FrmParAccess) access;
                frame = parAcc.frame;
                offset = new ImcCONST(parAcc.offset);

            } else if (access instanceof FrmLocAccess) {
                FrmLocAccess locAcc = (FrmLocAccess) access;
                frame = locAcc.frame;
                offset = new ImcCONST(locAcc.offset);
            }

            // get static link location
            fp = getStaticLink(frames.peek(), frame, fp);

            // calculate var location
            ImcExpr location = new ImcBINOP(ImcBINOP.ADD, fp, offset);

            mem = new ImcMEM(location);
        }

        imPart.put(acceptor, mem);
    }

    @Override
    public void visit(AbsWhere acceptor) {
        acceptor.defs.accept(this);
        acceptor.expr.accept(this);

        imPart.put(acceptor, imPart.get(acceptor.expr));
    }

    @Override
    public void visit(AbsWhile acceptor) {
        acceptor.cond.accept(this);
        acceptor.body.accept(this);

        FrmLabel condLabel = FrmLabel.newLabel();
        FrmLabel bodyLabel = FrmLabel.newLabel();
        FrmLabel endLabel = FrmLabel.newLabel();

        ImcSEQ seq = new ImcSEQ();

        // mark condition position
        seq.stmts.add(new ImcLABEL(condLabel));

        ImcExpr cond = (ImcExpr) imPart.get(acceptor.cond);

        // set condition true nad false labels
        seq.stmts.add(new ImcCJUMP(cond, bodyLabel, endLabel));

        // mark body position
        seq.stmts.add(new ImcLABEL(bodyLabel));

        ImcCode expr = imPart.get(acceptor.body);
        addExprStmts(seq, expr);

        // jump to condition (start of while loop) after body expr
        seq.stmts.add(new ImcJUMP(condLabel));

        // mark end position
        seq.stmts.add(new ImcLABEL(endLabel));

        imPart.put(acceptor, seq);
    }


    @Override
    public void visit(AbsUnExpr acceptor) {
        acceptor.expr.accept(this);
        ImcCode imc = null;
        ImcExpr expr = (ImcExpr) imPart.get(acceptor.expr);
        switch (acceptor.oper) {
            case AbsUnExpr.ADD:
                // 0 + expression
                imc = new ImcBINOP(ImcBINOP.ADD, new ImcCONST(0), expr);
                break;
            case AbsUnExpr.NOT:
                // false == false -> true, false == true -> false
                imc = new ImcBINOP(ImcBINOP.EQU, new ImcCONST(0), expr);
                break;
            case AbsUnExpr.SUB:
                // 0 - expression
                imc = new ImcBINOP(ImcBINOP.SUB, new ImcCONST(0), expr);
                break;
        }

        imPart.put(acceptor, imc);
    }

    // empty

    @Override
    public void visit(AbsArrType acceptor) {

    }


    @Override
    public void visit(AbsAtomType acceptor) {

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
}
