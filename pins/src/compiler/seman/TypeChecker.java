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

    private int phase;

    @Override
    public void visit(AbsDefs acceptor) {
        // type defs
        phase = 0;
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsTypeDef) {
                def.accept(this);
            }
        }

        // type defs in depth
        phase = 1;
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsTypeDef) {
                def.accept(this);
            }
        }

        // var defs
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsVarDef) {
                def.accept(this);
            }
        }

        // fun defs
        phase = 0;
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsFunDef) {
                def.accept(this);
            }
        }

        // fun in depth
        phase = 1;
        for (int i = 0; i < acceptor.numDefs(); i++) {
            AbsDef def = acceptor.def(i);
            if (def instanceof AbsFunDef) {
                def.accept(this);
            }
        }
    }

    @Override
    public void visit(AbsArrType acceptor) {
        acceptor.type.accept(this);
        SymbDesc.setType(acceptor, new SemArrType(acceptor.length, SymbDesc.getType(acceptor.type)));
    }

    @Override
    public void visit(AbsAtomConst acceptor) {
        SymbDesc.setType(acceptor, new SemAtomType(acceptor.type));
    }

    @Override
    public void visit(AbsAtomType acceptor) {
        SymbDesc.setType(acceptor, new SemAtomType(acceptor.type));
    }

    @Override
    public void visit(AbsBinExpr acceptor) {
        // check expression types
        acceptor.expr1.accept(this);
        acceptor.expr2.accept(this);

        // get expressions types
        SemType expr1T = SymbDesc.getType(acceptor.expr1);
        SemType expr2T = SymbDesc.getType(acceptor.expr2);

        switch (acceptor.oper) {
            case 0:
            case 1:
                // check if types are same
                if (expr1T.sameStructureAs(expr2T)) {
                    // check if type is SemAtomType
                    if (expr1T.actualType() instanceof SemAtomType) {
                        int type1 = ((SemAtomType) expr1T.actualType()).type;
                        int type2 = ((SemAtomType) expr2T.actualType()).type;
                        // check if types are logical
                        if (type1 == 0 && type2 == 0) {
                            SymbDesc.setType(acceptor, new SemAtomType(0));
                        } else {
                            Report.error(acceptor.position, "expected logic type");
                        }
                    } else {
                        Report.error(acceptor.position, "expected logic type");
                    }
                } else {
                    Report.error(acceptor.position, "expected same (logic) types");
                }
                break;

            // ==, !=, <=, >=, <,>
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                // check if types are same
                if (expr1T.sameStructureAs(expr2T)) {
                    // check if type is SemAtomType
                    if (expr1T.actualType() instanceof SemAtomType) {
                        int type1 = ((SemAtomType) expr1T.actualType()).type;
                        int type2 = ((SemAtomType) expr2T.actualType()).type;
                        // check if types are logical
                        if (type1 == 0 && type2 == 0) {
                            SymbDesc.setType(acceptor, new SemAtomType(0));
                        }
                        // check if types are integer
                        else if (type1 == 1 && type2 == 1){
                            SymbDesc.setType(acceptor, new SemAtomType(0));
                        } else {
                            Report.error(acceptor.position, "expected logic or int type");
                        }
                    } else {
                        Report.error(acceptor.position, "expected logic or int type");
                    }
                } else {
                    Report.error(acceptor.position, "expected same (logic or int) types");
                }
                break;

            // +, -, *, /, %
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
                // check if types are same
                if (expr1T.sameStructureAs(expr2T)) {
                    // check if type is SemAtomType
                    if (expr1T.actualType() instanceof SemAtomType) {
                        int type1 = ((SemAtomType) expr1T.actualType()).type;
                        int type2 = ((SemAtomType) expr2T.actualType()).type;
                        // check if types are integer
                        if (type1 == 1 && type2 == 1) {
                            SymbDesc.setType(acceptor, new SemAtomType(1));
                        } else {
                            Report.error(acceptor.position, "expected integer type");
                        }
                    } else {
                        Report.error(acceptor.position, "expected integer type");
                    }
                } else {
                    Report.error(acceptor.position, "expected same integer type");
                }
                break;
            // array []
            case 14:
                if (expr1T.actualType() instanceof SemArrType) {
                    SemArrType arr = (SemArrType) expr1T.actualType();

                    // check if expr2 is integer
                    if (expr2T.actualType() instanceof SemAtomType) {
                        int type = ((SemAtomType) expr2T.actualType()).type;
                        if (type == 1) {
                            SymbDesc.setType(acceptor, arr.type.actualType());
                        } else {
                            Report.error(acceptor.position, "expected integer type");
                        }
                    } else {
                        Report.error(acceptor.position, "expected integer type");
                    }
                } else {
                    Report.error(acceptor.position, "expected array type");
                }
                break;
            // =
            case 15:
                // check if types are same
                if (expr1T.sameStructureAs(expr2T)) {
                    if (expr2T.actualType() instanceof SemAtomType) {
                        int type1 = ((SemAtomType) expr1T.actualType()).type;
                        int type2 = ((SemAtomType) expr2T.actualType()).type;
                        // check if types are logical
                        if (type1 == 0 && type2 == 0) {
                            SymbDesc.setType(acceptor, new SemAtomType(0));
                        }
                        // check if types are integer
                        else if (type1 == 1 && type2 == 1) {
                            SymbDesc.setType(acceptor, new SemAtomType(1));
                        }
                        // check if types are string
                        else if (type1 == 2 && type2 == 2) {
                            SymbDesc.setType(acceptor, new SemAtomType(2));
                        } else {
                            Report.error(acceptor.position, "expected logic, integer or string type");
                        }
                    } else {
                        Report.error(acceptor.position, "expected logic, integer or string type");
                    }
                } else {
                    Report.error(acceptor.position, "expected same types (logic, integer or string)");
                }
        }
    }

    @Override
    public void visit(AbsExprs acceptor) {
        // visit all expressions
        for (int i = 0; i < acceptor.numExprs(); i++) {
            acceptor.expr(i).accept(this);
        }
        // type of last expression is type of expressions
        SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.expr(acceptor.numExprs()-1)));
    }

    @Override
    public void visit(AbsFor acceptor) {
        acceptor.count.accept(this);
        acceptor.lo.accept(this);
        acceptor.hi.accept(this);
        acceptor.step.accept(this);
        acceptor.body.accept(this);

        SemType lo = SymbDesc.getType(acceptor.lo);
        SemType hi = SymbDesc.getType(acceptor.lo);
        SemType step = SymbDesc.getType(acceptor.lo);

        if (lo.actualType() instanceof SemAtomType) {
            // report error if type isn't int
            if (((SemAtomType) lo.actualType()).type != 1) {
                Report.error(acceptor.position, "expected int");
            }
        } else {
            Report.error(acceptor.position, "expected int");
        }

        if (hi.actualType() instanceof SemAtomType) {
            // report error if type isn't int
            if (((SemAtomType) hi.actualType()).type != 1) {
                Report.error(acceptor.position, "expected int");
            }
        } else {
            Report.error(acceptor.position, "expected int");
        }

        if (step.actualType() instanceof SemAtomType) {
            // report error if type isn't int
            if (((SemAtomType) step.actualType()).type != 1) {
                Report.error(acceptor.position, "expected int");
            }
        } else {
            Report.error(acceptor.position, "expected int");
        }

        // set void type
        SymbDesc.setType(acceptor, new SemAtomType(3));
    }

    @Override
    public void visit(AbsFunCall acceptor) {
        AbsFunDef funDef = (AbsFunDef) SymbDesc.getNameDef(acceptor);
        SemFunType funType = (SemFunType) SymbDesc.getType(funDef);
        SymbDesc.setType(acceptor, funType.resultType);

        if (acceptor.numArgs() != funDef.numPars()) {
            Report.error(acceptor.position, "wrong number of arguments");
        }

        for (int i = 0; i < funDef.numPars(); i++) {
            acceptor.arg(i).accept(this);
            // check if argument and parameter types match
            if (!SymbDesc.getType(funDef.par(i)).sameStructureAs(SymbDesc.getType(acceptor.arg(i)))) {
                Report.error(acceptor.position, "wrong argument types");
            }
        }
    }

    @Override
    public void visit(AbsFunDef acceptor) {
        if (phase == 0) {
            acceptor.type.accept(this);

            Vector<SemType> parTypes = new Vector<>();
            for (int i = 0; i < acceptor.numPars(); i++) {
                acceptor.par(i).accept(this);
                parTypes.add(SymbDesc.getType(acceptor.par(i).type));
            }

            SymbDesc.setType(acceptor, new SemFunType(parTypes, SymbDesc.getType(acceptor.type)));
        } else {
            acceptor.expr.accept(this);
            if (!SymbDesc.getType(acceptor.type).sameStructureAs(SymbDesc.getType(acceptor.expr))) {
                Report.error(acceptor.position, "wrong function return type");
            }
        }
    }

    @Override
    public void visit(AbsIfThen acceptor) {
        acceptor.cond.accept(this);
        acceptor.thenBody.accept(this);

        SemType cond = SymbDesc.getType(acceptor.cond);

        if (cond.actualType() instanceof SemAtomType) {
            // report error if type isn't logical
            if (((SemAtomType) cond.actualType()).type != 0) {
                Report.error(acceptor.position, "expected logical");
            }
        } else {
            Report.error(acceptor.position, "expected logical");
        }

        // set void type
        SymbDesc.setType(acceptor, new SemAtomType(3));
    }

    @Override
    public void visit(AbsIfThenElse acceptor) {
        acceptor.cond.accept(this);
        acceptor.thenBody.accept(this);
        acceptor.elseBody.accept(this);

        SemType cond = SymbDesc.getType(acceptor.cond);

        if (cond.actualType() instanceof SemAtomType) {
            // report error if type isn't logical
            if (((SemAtomType) cond.actualType()).type != 0) {
                Report.error(acceptor.position, "expected logical");
            }
        } else {
            Report.error(acceptor.position, "expected logical");
        }

        // set void type
        SymbDesc.setType(acceptor, new SemAtomType(3));
    }

    @Override
    public void visit(AbsPar acceptor) {
        acceptor.type.accept(this);
        SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.type));
    }

    @Override
    public void visit(AbsTypeDef acceptor) {
        if (phase == 0) {
            SymbDesc.setType(acceptor, new SemTypeName(acceptor.name));
        } else {
            acceptor.type.accept(this);
            SemTypeName t = (SemTypeName) SymbDesc.getType(acceptor);
            t.setType(SymbDesc.getType(acceptor.type));
        }
    }

    @Override
    public void visit(AbsTypeName acceptor) {
        SymbDesc.setType(acceptor, SymbDesc.getType(SymbDesc.getNameDef(acceptor)));
    }

    @Override
    public void visit(AbsUnExpr acceptor) {
        acceptor.expr.accept(this);

        // get expession type
        SemType exprT = SymbDesc.getType(acceptor.expr);

        switch (acceptor.oper) {
            // +, -
            case 0:
            case 1:
                if (exprT.actualType() instanceof SemAtomType) {
                    int type = ((SemAtomType) exprT.actualType()).type;
                    // check if type is integer
                    if (type == 1) {
                        SymbDesc.setType(acceptor, new SemAtomType(1));
                    } else {
                        Report.error(acceptor.position, "expected integer type");
                    }
                } else {
                    Report.error(acceptor.position, "expected integer type");
                }
                break;
            // !
            case 4:
                if (exprT.actualType() instanceof SemAtomType) {
                    int type = ((SemAtomType) exprT.actualType()).type;
                    // check if type is logical
                    if (type == 0) {
                        SymbDesc.setType(acceptor, new SemAtomType(0));
                    } else {
                        Report.error(acceptor.position, "expected logical type");
                    }
                } else {
                    Report.error(acceptor.position, "expected logical type");
                }
        }
    }

    @Override
    public void visit(AbsVarDef acceptor) {
        acceptor.type.accept(this);
        SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.type));
    }

    @Override
    public void visit(AbsVarName acceptor) {
        // set type to type of definition
        SymbDesc.setType(acceptor, SymbDesc.getType(SymbDesc.getNameDef(acceptor)));
    }

    @Override
    public void visit(AbsWhere acceptor) {
        acceptor.defs.accept(this);
        acceptor.expr.accept(this);

        // set same type as expr
        SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.expr));

    }

    @Override
    public void visit(AbsWhile acceptor) {
        acceptor.cond.accept(this);
        acceptor.body.accept(this);

        SemType cond = SymbDesc.getType(acceptor.cond);

        if (cond.actualType() instanceof SemAtomType) {
            // report error if type isn't logical
            if (((SemAtomType) cond.actualType()).type != 0) {
                Report.error(acceptor.position, "expected logical");
            }
        } else {
            Report.error(acceptor.position, "expected logical");
        }

        // set void type
        SymbDesc.setType(acceptor, new SemAtomType(3));
    }
}
