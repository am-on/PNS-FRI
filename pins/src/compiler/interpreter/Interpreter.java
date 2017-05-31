package compiler.interpreter;

import compiler.Report;
import compiler.frames.FrmTemp;
import compiler.imcode.*;
import java.util.*;

import compiler.frames.*;

import java.util.Scanner;

public class Interpreter {

	public static boolean debug = false;

	/*--- staticni del navideznega stroja ---*/

	/** Pomnilnik navideznega stroja. */
	public static HashMap<Integer, Object> mems = new HashMap<Integer, Object>();


	public static void stM(Integer address, Object value) {
		if (debug) System.out.println(" [" + address + "] <= " + value);
		mems.put(address, value);
	}

	public static Object ldM(Integer address) {
		Object value = mems.get(address);
		if (debug) System.out.println(" [" + address + "] => " + value);
		return value;
	}

	/** Kazalec na vrh klicnega zapisa. */
	private static int fp = 1000;

	/** Kazalec na dno klicnega zapisa. */
	private static int sp = 1000;

	private ImcCodeGen imcCodeGen = null;

	/*--- dinamicni del navideznega stroja ---*/

	/** Zacasne spremenljivke (`registri') navideznega stroja. */
	public HashMap<FrmTemp, Object> temps = new HashMap<FrmTemp, Object>();

	public void stT(FrmTemp temp, Object value) {
		if (debug) System.out.println(" " + temp.name() + " <= " + value);
		temps.put(temp, value);
	}

	public Object ldT(FrmTemp temp) {
		Object value = temps.get(temp);
		if (debug) System.out.println(" " + temp.name() + " => " + value);
		return value;
	}

	public Interpreter(ImcCodeGen imcCodeGen) {
		this.imcCodeGen = imcCodeGen;

		linearizator(imcCodeGen);

		String defaultFunction = "main";

		interpretFunctionName(defaultFunction);
	}

	private void interpretFunctionName(String funName) {
		FrmFrame frame = null;
		ImcSEQ code = null;

		for (int i = 0; i < imcCodeGen.chunks.size(); i++) {
			ImcChunk c = imcCodeGen.chunks.get(i);
			if (c instanceof  ImcCodeChunk) {
				frame = ((ImcCodeChunk)c).frame;
				if (frame.fun.name.equals(funName)) {
					code = ((ImcCodeChunk) c).lincode.linear();
					break;
				}
			}
		}

		interpret(frame, code);
	}

	public Interpreter(String funLabel, ImcCodeGen imcCodeGen) {
		this.imcCodeGen = imcCodeGen;
		FrmFrame frame = null;
		ImcSEQ code = null;

		for (int i = 0; i < imcCodeGen.chunks.size(); i++) {
			ImcChunk c = imcCodeGen.chunks.get(i);
			if (c instanceof  ImcCodeChunk) {
				frame = ((ImcCodeChunk)c).frame;
				if (frame.label.name.equals(funLabel)) {
					code = ((ImcCodeChunk) c).lincode.linear();
					break;
				}
			}
		}

		interpret(frame, code);
	}

	private void linearizator(ImcCodeGen imcCodeGen) {

		for(int i = 0; i < imcCodeGen.chunks.size(); i++) {
			ImcChunk ch = imcCodeGen.chunks.get(i);
			if(ch instanceof ImcCodeChunk) {
				ImcCodeChunk c = (ImcCodeChunk)ch;
				c.lincode = c.imcode.linear();
				imcCodeGen.chunks.set(i, c);
			}
		}
	}

	/*--- Izvajanje navideznega stroja. ---*/

	private void interpret(FrmFrame frame, ImcSEQ code) {
		if (debug) {
			System.out.println("[START OF " + frame.label.name() + "]");
		}

		stM(sp - frame.sizeLocs - 4 , fp);
		fp = sp;
		sp = sp - frame.size();


		if (debug) {
			System.out.println("[FP=" + fp + "]");
			System.out.println("[SP=" + sp + "]");
		}

		stT(frame.FP, fp);



		// set argument of main function to 0
		if(frame.fun.name.equals("main")){
			stM(fp+4, 0);
		}

		int pc = 0;
		Object result = null;
		while (pc < code.stmts.size()) {
			if (debug) System.out.println("pc=" + pc);
			ImcCode instruction = code.stmts.get(pc);
			result = execute(instruction);
			if (result instanceof ImcLABEL) {
				for (pc = 0; pc < code.stmts.size(); pc++) {
					instruction = code.stmts.get(pc);
					if ((instruction instanceof ImcLABEL)
							&& (((ImcLABEL) instruction).label.name().equals(((ImcLABEL) result).label.name())))
						break;
				}
			}
			else
				pc++;
		}

		sp = sp + frame.size();
		fp = (Integer) ldM(sp - frame.sizeLocs - 4 );

		if (debug) {
			System.out.println("[FP=" + fp + "]");
			System.out.println("[SP=" + sp + "]");
		}

		stM(sp, result);
		if (debug) {
			System.out.println("[RV=" + result + "]");
		}

		if (debug) {
			System.out.println("[END OF " + frame.label.name() + "]");
		}
	}

	public Object execute(ImcCode instruction) {

		if (instruction instanceof ImcBINOP) {
			ImcBINOP instr = (ImcBINOP) instruction;
			Object fstSubValue = execute(instr.limc);
			Object sndSubValue = execute(instr.rimc);
			switch (instr.op) {
				case ImcBINOP.OR:
					return ((((Integer) fstSubValue).intValue() != 0) || (((Integer) sndSubValue).intValue() != 0) ? 1 : 0);
				case ImcBINOP.AND:
					return ((((Integer) fstSubValue).intValue() != 0) && (((Integer) sndSubValue).intValue() != 0) ? 1 : 0);
				case ImcBINOP.EQU:
					return (((Integer) fstSubValue).intValue() == ((Integer) sndSubValue).intValue() ? 1 : 0);
				case ImcBINOP.NEQ:
					return (((Integer) fstSubValue).intValue() != ((Integer) sndSubValue).intValue() ? 1 : 0);
				case ImcBINOP.LTH:
					return (((Integer) fstSubValue).intValue() < ((Integer) sndSubValue).intValue() ? 1 : 0);
				case ImcBINOP.GTH:
					return (((Integer) fstSubValue).intValue() > ((Integer) sndSubValue).intValue() ? 1 : 0);
				case ImcBINOP.LEQ:
					return (((Integer) fstSubValue).intValue() <= ((Integer) sndSubValue).intValue() ? 1 : 0);
				case ImcBINOP.GEQ:
					return (((Integer) fstSubValue).intValue() >= ((Integer) sndSubValue).intValue() ? 1 : 0);
				case ImcBINOP.ADD:
					return (((Integer) fstSubValue).intValue() + ((Integer) sndSubValue).intValue());
				case ImcBINOP.SUB:
					return (((Integer) fstSubValue).intValue() - ((Integer) sndSubValue).intValue());
				case ImcBINOP.MUL:
					return (((Integer) fstSubValue).intValue() * ((Integer) sndSubValue).intValue());
				case ImcBINOP.DIV:
					return (((Integer) fstSubValue).intValue() / ((Integer) sndSubValue).intValue());
				case ImcBINOP.MOD:
					return (((Integer) fstSubValue).intValue() % ((Integer) sndSubValue).intValue());
//			case ImcBINOP.EQU:
//				return (((String) fstSubValue).compareTo((String) sndSubValue)) == 0 ? 1 : 0;
//			case ImcBINOP.NEQ:
//				return (((String) fstSubValue).compareTo((String) sndSubValue)) != 0 ? 1 : 0;
//			case ImcBINOP.LTH:
//				return (((String) fstSubValue).compareTo((String) sndSubValue)) < 0 ? 1 : 0;
//			case ImcBINOP.GTH:
//				return (((String) fstSubValue).compareTo((String) sndSubValue)) > 0 ? 1 : 0;
//			case ImcBINOP.LEQ:
//				return (((String) fstSubValue).compareTo((String) sndSubValue)) <= 0 ? 1 : 0;
//			case ImcBINOP.GEQ:
//				return (((String) fstSubValue).compareTo((String) sndSubValue)) >= 0 ? 1 : 0;
			}
			Report.error("Internal error.");
			return null;
		}

		if (instruction instanceof ImcCALL) {
			ImcCALL instr = (ImcCALL) instruction;
			int offset = 0;
			for (ImcCode arg : instr.args) {
				stM(sp + offset, execute(arg));
				offset += 4;
			}
			if (instr.label.name().equals("_putInt")) {
				System.out.println((Integer) ldM(sp + 4));
				return null;
			}
			if (instr.label.name().equals("_getInt")) {
				Scanner scanner = new Scanner(System.in);
				stM((Integer) ldM (sp + 4),scanner.nextInt());
				return null;
			}
			if (instr.label.name().equals("_putString")) {
				System.out.println((String) ldM(sp + 4));
				return null;
			}
			if (instr.label.name().equals("_getString")) {
				Scanner scanner = new Scanner(System.in);
				stM((Integer) ldM (sp + 4),scanner.next());
				return null;
			}

			new Interpreter(instr.label.name(), this.imcCodeGen);

			return ldM(sp);
		}

		if (instruction instanceof ImcCJUMP) {
			ImcCJUMP instr = (ImcCJUMP) instruction;
			Object cond = execute(instr.cond);
			if (cond instanceof Integer) {
				if (((Integer) cond).intValue() != 0)
					return new ImcLABEL(instr.trueLabel);
				else
					return new ImcLABEL(instr.falseLabel);
			}
			else Report.error("CJUMP: illegal condition type.");
		}

		if (instruction instanceof ImcCONST) {
			ImcCONST instr = (ImcCONST) instruction;
			return new Integer(instr.value);
		}

//		if (instruction instanceof ImcCONSTs) {
//			ImcCONSTs instr = (ImcCONSTs) instruction;
//			return new String(instr.stringValue);
//		}

		if (instruction instanceof ImcJUMP) {
			ImcJUMP instr = (ImcJUMP) instruction;
			return new ImcLABEL(instr.label);
		}

		if (instruction instanceof ImcLABEL) {
			return null;
		}

		if (instruction instanceof ImcMEM) {
			ImcMEM instr = (ImcMEM) instruction;
			return ldM((Integer) execute(instr.expr));
		}

		if (instruction instanceof ImcMOVE) {
			ImcMOVE instr = (ImcMOVE) instruction;
			if (instr.dst instanceof ImcTEMP) {
				FrmTemp temp = ((ImcTEMP) instr.dst).temp;
				Object srcValue = execute(instr.src);
				stT(temp, srcValue);
				return srcValue;
			}
			if (instr.dst instanceof ImcMEM) {
				Object dstValue = execute(((ImcMEM) instr.dst).expr);
				Object srcValue = execute(instr.src);
				stM((Integer) dstValue, srcValue);
				return srcValue;
			}
		}

		if (instruction instanceof ImcNAME) {
			String instrLabel = ((ImcNAME) instruction).label.name();
			if (instrLabel.equals("FP")) return fp;
			if (instrLabel.equals("SP")) return sp;

		}

		if (instruction instanceof ImcTEMP) {
			ImcTEMP instr = (ImcTEMP) instruction;
			return ldT(instr.temp);
		}

//		if (instruction instanceof ImcUNOP) {
//			ImcUNOP instr = (ImcUNOP) instruction;
//			Object subValue = execute(instr.subExpr);
//			switch (instr.oper) {
//			case ImcUNOP.ADDi:
//				return +(((Integer) subValue).intValue());
//			case ImcUNOP.SUBi:
//				return -(((Integer) subValue).intValue());
//			case ImcUNOP.ADDr:
//				return +(((Float) subValue).floatValue());
//			case ImcUNOP.SUBr:
//				return +(((Float) subValue).floatValue());
//			case ImcUNOP.NOT:
//				return (((Integer) subValue).intValue() == 0 ? 1 : 0);
//			}
//			Report.error("Internal error.", 1);
//			return null;
//		}

		return null;
	}

}
