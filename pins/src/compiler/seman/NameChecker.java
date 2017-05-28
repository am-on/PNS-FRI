package compiler.seman;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 * @author sliva
 */
public class NameChecker implements Visitor {


    public NameChecker() {

    }
    @Override
    public void visit(AbsArrType acceptor) {
        acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsAtomConst acceptor) {

    }

    @Override
    public void visit(AbsAtomType acceptor) {

    }

    @Override
    public void visit(AbsBinExpr acceptor) {
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);
    }

    @Override
    public void visit(AbsDefs acceptor) {

        // save type definitions to SymbTable
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsTypeDef) {
                AbsTypeDef td = (AbsTypeDef) def;
                try {
                    SymbTable.ins(td.name, def);
                } catch (SemIllegalInsertException e) {
                    Report.error(td.position,"Duplicate AbsTypeDef: " + td.name);
                }
            }
        }

        // check if type names are in SymbTable
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsTypeDef) {
                AbsTypeDef td = (AbsTypeDef) def;
                td.accept(this);
            }
        }

        // add var names to SymbTable and check types
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsVarDef) {
                AbsVarDef vd = (AbsVarDef) def;
                vd.accept(this);
            }
        }

        // add fun names to SymbTable and check returning type and type of params and
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsFunDef) {
                AbsFunDef fd = (AbsFunDef) def;
                try {
                    SymbTable.ins(fd.name, def);
                } catch (SemIllegalInsertException e) {
                    Report.error(fd.position, "Duplicate AbsFunDef: " + fd.name);
                }

                // check param types
                for (int j = 0; j < fd.numPars(); j++) {
                    fd.par(j).type.accept(this);
                }

                // check returning type
                fd.type.accept(this);

            }
        }

        // fun in depth
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsFunDef) {
                AbsFunDef fd = (AbsFunDef) def;
                fd.accept(this);
            }
        }

    }

    @Override
    public void visit(AbsExprs acceptor) {
        for (int i = 0; i < acceptor.numExprs(); i++) {
            acceptor.expr(i).accept(this);
        }
    }

    @Override
    public void visit(AbsFor acceptor) {
        acceptor.count.accept(this);
        acceptor.hi.accept(this);
        acceptor.lo.accept(this);
        acceptor.step.accept(this);
        acceptor.body.accept(this);
    }

    @Override
    public void visit(AbsFunCall acceptor) {
        // TODO skip predefined functions

        // check if function is defined
        if (SymbTable.fnd(acceptor.name) == null) {
            Report.error(acceptor.position, "Call to undefined function " + acceptor.name);
        }

        SymbDesc.setNameDef(acceptor, SymbTable.fnd(acceptor.name));

        // check function args
        for (int i = 0; i < acceptor.numArgs(); i++) {
            acceptor.arg(i).accept(this);
        }
    }

    @Override
    public void visit(AbsFunDef acceptor) {
        acceptor.type.accept(this);


        SymbTable.newScope();

        for (int i = 0; i < acceptor.numPars(); i++) {
            acceptor.par(i).accept(this);
        }

        acceptor.expr.accept(this);

        SymbTable.oldScope();
    }

    @Override
    public void visit(AbsIfThen accpetor) {
        accpetor.cond.accept(this);
        accpetor.thenBody.accept(this);
    }

    @Override
    public void visit(AbsIfThenElse accpetor) {
        accpetor.cond.accept(this);
        accpetor.thenBody.accept(this);
        accpetor.elseBody.accept(this);
    }

    @Override
    public void visit(AbsPar acceptor) {
        try {
            SymbTable.ins(acceptor.name, acceptor);
        } catch (SemIllegalInsertException e) {
            Report.error(acceptor.position, "Duplicate parameter name: " + acceptor.name);
        }
    }

    @Override
    public void visit(AbsTypeDef acceptor) {
        acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsTypeName acceptor) {
        if (SymbTable.fnd(acceptor.name) == null)
            Report.error(acceptor.position, "Undefined type: " + acceptor.name);

        SymbDesc.setNameDef(acceptor, SymbTable.fnd(acceptor.name));
    }

    @Override
    public void visit(AbsUnExpr acceptor) {
        acceptor.expr.accept(this);
    }

    @Override
    public void visit(AbsVarDef acceptor) {
        try {
            // insert var name to SymbTable
            SymbTable.ins(acceptor.name, acceptor);
        } catch (SemIllegalInsertException e) {
            Report.error(acceptor.position, "Duplicate AbsVarDef: " + acceptor.name);
        }

        // check var type
        acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsVarName acceptor) {
        if (SymbTable.fnd(acceptor.name) == null)
            Report.error(acceptor.position, "Undefined var: " + acceptor.name);

        SymbDesc.setNameDef(acceptor, SymbTable.fnd(acceptor.name));
    }

    @Override
    public void visit(AbsWhere acceptor) {
        SymbTable.newScope();
        acceptor.defs.accept(this);
        acceptor.expr.accept(this);
        SymbTable.oldScope();

    }

    @Override
    public void visit(AbsWhile acceptor) {
        acceptor.cond.accept(this);
        acceptor.body.accept(this);
    }


}
