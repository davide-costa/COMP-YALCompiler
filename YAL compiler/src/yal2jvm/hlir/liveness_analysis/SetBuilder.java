package yal2jvm.hlir.liveness_analysis;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.TreeSet;

import yal2jvm.Yal2jvm;
import yal2jvm.hlir.IRAllocate;
import yal2jvm.hlir.IRArith;
import yal2jvm.hlir.IRCall;
import yal2jvm.hlir.IRComparison;
import yal2jvm.hlir.IRJump;
import yal2jvm.hlir.IRLabel;
import yal2jvm.hlir.IRLoad;
import yal2jvm.hlir.IRLoadArith;
import yal2jvm.hlir.IRMethod;
import yal2jvm.hlir.IRModule;
import yal2jvm.hlir.IRNode;
import yal2jvm.hlir.IRReturn;
import yal2jvm.hlir.IRStoreArith;
import yal2jvm.hlir.IRStoreCall;
import yal2jvm.hlir.Variable;
import yal2jvm.utils.Utils;

/**
 * Class to perform dataflow and liveness analysis on a single method. It
 * calculates DEF, USE, IN, OUT and SUCC sets in order to determine which are
 * the interferences between variables, producing an interference graph holding
 * that information
 */
public class SetBuilder
{
	private IRMethod node;
	private HashMap<String, Integer> varToBit;
	private ArrayList<Line> lines;
	private ArrayList<String> locals;
	private int lineCount = 0;

	/**
	 * Constructor
	 * 
	 * @param method
	 *            HLIR node that represents the whole method
	 */
	public SetBuilder(IRMethod method)
	{
		this.node = method;
		this.lines = new ArrayList<>();
		this.varToBit = new HashMap<>();
	}

	/**
	 * Gets a list with all the local variables of the method, including arguments.
	 * It also creates a mapping between each variable and sequential integers
	 * 
	 * @return list with the local variables
	 */
	public ArrayList<String> getAllVars()
	{
		IRModule module = (IRModule) this.node.findParent("Module");
		TreeSet<String> globals = module.getAllGlobals();
		TreeSet<String> locals = findLocals();
		locals.removeAll(globals);
		ArrayList<String> list = Utils.setToList(locals);

		for (int i = 0; i < list.size(); i++)
			varToBit.put(list.get(i), i);

		this.locals = list;
		return list;
	}

	/**
	 * Finds all the local variables of the method, including arguments
	 * 
	 * @return list with all the local variables
	 */
	private TreeSet<String> findLocals()
	{
		TreeSet<String> locals = new TreeSet<String>();

		for (IRNode n : this.node.getChildren())
		{
			switch (n.getNodeType())
			{
			case "Allocate":
			{
				IRAllocate alloc = (IRAllocate) n;
				locals.add(alloc.getName());
				if (alloc.getRhs().getNodeType().equals("Load"))
				{
					IRLoad load = (IRLoad) alloc.getRhs();
					locals.add(load.getName());
				}
				break;
			}
			case "StoreArith":
			{
				IRStoreArith arith = (IRStoreArith) n;
				locals.add(arith.getName());
				break;
			}
			case "StoreCall":
			{
				IRStoreCall call = (IRStoreCall) n;
				locals.add(call.getName());
				break;
			}
			}
		}
		Variable[] args = this.node.getArgs();
		for (Variable arg : args)
			locals.add(arg.getVar());

		return locals;
	}

	/**
	 * Gets a method line
	 * 
	 * @param id
	 *            the number of the line
	 * @return the line
	 */
	public Line getLine(int id)
	{
		for (Line line : this.lines)
		{
			if (line.getId() == id)
				return line;
		}
		return null;
	}

	/**
	 * Gets the method name
	 * 
	 * @return the method name
	 */
	public String getName()
	{
		return this.node.getName();
	}

	/**
	 * Builds all method lines based on the HLIR of the method
	 */
	public void buildAllLines()
	{
		this.lines.add(createMethodArgumentsLine());

		for (IRNode n : this.node.getChildren())
		{
			Line line = new Line(this.lineCount, this.varToBit);
			this.lineCount++;

			switch (n.getNodeType())
			{
			case "Allocate":
				buildLineAllocate((IRAllocate) n, line);
				break;
			case "StoreArith":
				buildLineStoreArith((IRStoreArith) n, line);
				break;
			case "StoreCall":
				buildLineStoreCall((IRStoreCall) n, line);
				break;
			case "Return":
				buildLineReturn((IRReturn) n, line);
				break;
			case "Comparison":
				buildLineComparison((IRComparison) n, line);
				break;
			case "Call":
				buildLineCall((IRCall) n, line);
				break;
			case "Label":
				buildLineLabel((IRLabel) n, line);
				break;
			case "Jump":
				buildLineJump((IRJump) n, line);
				break;
			}
			this.lines.add(line);
		}
		setSuccessors();
	}

	/**
	 * Creates a special line for the arguments of the method
	 * 
	 * @return a line for the arguments of the method
	 */
	private Line createMethodArgumentsLine()
	{
		Line line = new Line(this.lineCount, this.varToBit);
		this.lineCount++;

		Variable[] args = this.node.getArgs();
		for (Variable arg : args)
			line.addDef(arg.getVar());

		return line;
	}

	/**
	 * Sets the successors to all the generated lines. Lines can have: - 0
	 * successors (for example, the return line of a method) - 1 successor (direct
	 * successor or a jump to another line) - 2 successors (direct successor and a
	 * jump to another line, like in a comparison)
	 */
	private void setSuccessors()
	{
		for (int i = 0; i < this.lines.size() - 1; i++)
		{
			Line currLine = this.lines.get(i);

			if (currLine.isJump())
			{
				Line dest = findLabelLine(currLine.getJumpLabel());
				currLine.addSuccessor(dest);
			}
			if (currLine.hasSuccessor())
			{
				currLine.addSuccessor(this.lines.get(i + 1));
			}
		}
	}

	/**
	 * Finds the line with the specified label
	 * 
	 * @param label
	 *            the label
	 * @return the line with the specified label
	 */
	private Line findLabelLine(String label)
	{
		for (Line line : this.lines)
		{
			if (line.getLabel().equals(label))
				return line;
		}
		return null;
	}

	/**
	 * Builds a line based on an IRJump HLIR node
	 * 
	 * @param node
	 *            the node
	 * @param line
	 *            the line to build
	 */
	private void buildLineJump(IRJump node, Line line)
	{
		line.setJump(true);
		line.setJumpLabel(node.getLabel());
		line.setHasSuccessor(false);
		line.setType("Jump");
	}

	/**
	 * Builds a line based on an IRLabel HLIR node
	 * 
	 * @param node
	 *            the node
	 * @param line
	 *            the line to build
	 */
	private void buildLineLabel(IRLabel node, Line line)
	{
		line.addLabel(node.getLabel());
		line.setType("Label");
	}

	/**
	 * Builds a line based on an IRCall HLIR node
	 * 
	 * @param node
	 *            the node
	 * @param line
	 *            the line to build
	 */
	private void buildLineCall(IRCall node, Line line)
	{
		ArrayList<Variable> args = node.getArguments();
		for (Variable arg : args)
		{
			if (isNotGlobal(arg.getVar()))
				line.addUse(arg.getVar());
		}
		line.setType("Call");
	}

	/**
	 * Builds a line based on an IRComparison HLIR node
	 * 
	 * @param node
	 *            the node
	 * @param line
	 *            the line to build
	 */
	private void buildLineComparison(IRComparison node, Line line)
	{
		line.setJump(true);
		line.setJumpLabel(node.getLabel());
		line.setType("Comp");
		
		if (node.getRhs().getNodeType().equals("Load"))
		{
			IRLoad load = (IRLoad) node.getRhs();
			if (isNotGlobal(load.getName()))
			{
				line.addUse(load.getName());
			}
		}
		if (node.getLhs().getNodeType().equals("Load"))
		{
			IRLoad load = (IRLoad) node.getLhs();

			if (isNotGlobal(load.getName()))
			{
				line.addUse(load.getName());
			}
		}
		if (node.getRhs().getNodeType().equals("LoadArith"))
		{
			IRLoadArith arith = (IRLoadArith) node.getRhs();
			
			if (arith.getRhs().getNodeType().equals("Load"))
			{
				IRLoad load = (IRLoad) arith.getRhs();
				if (isNotGlobal(load.getName()))
				{
					line.addUse(load.getName());
				}
			}
			if (arith.getLhs().getNodeType().equals("Load"))
			{
				IRLoad load = (IRLoad) arith.getLhs();
				if (isNotGlobal(load.getName()))
				{
					line.addUse(load.getName());
				}
			}
		}
	}

	/**
	 * Builds a line based on an IRReturn HLIR node
	 * 
	 * @param node
	 *            the node
	 * @param line
	 *            the line to build
	 */
	private void buildLineReturn(IRReturn node, Line line)
	{
		if (isNotGlobal(node.getName()))
		{
			line.addUse(node.getName());
			line.setType("Return");
		}
	}

	/**
	 * Builds a line based on an IRStoreCall HLIR node
	 * 
	 * @param node
	 *            the node
	 * @param line
	 *            the line to build
	 */
	private void buildLineStoreCall(IRStoreCall node, Line line)
	{
		line.setType("StoreCall");
		if (isNotGlobal(node.getName()))
		{
			line.addDef(node.getName());
		}
		IRCall call = (IRCall) node.getChildren().get(0);
		ArrayList<Variable> args = call.getArguments();
		for (Variable arg : args)
		{
			if (isNotGlobal(arg.getVar()))
				line.addUse(arg.getVar());
		}

	}

	/**
	 * Builds a line based on an IRStoreArith HLIR node
	 * 
	 * @param node
	 *            the node
	 * @param line
	 *            the line to build
	 */
	private void buildLineStoreArith(IRStoreArith node, Line line)
	{
		line.setType("StoreArith");
		if (isNotGlobal(node.getName()))
		{
			line.addDef(node.getName());
		}
		if (node.getRhs().getNodeType().equals("Load"))
		{
			IRLoad load = (IRLoad) node.getRhs();
			if (isNotGlobal(load.getName()))
			{
				line.addUse(load.getName());
			}
		}
		if (node.getLhs().getNodeType().equals("Load"))
		{
			IRLoad load = (IRLoad) node.getLhs();
			if (isNotGlobal(load.getName()))
			{
				line.addUse(load.getName());
			}
		}
	}

	/**
	 * Builds a line based on an IRAllocate HLIR node
	 * 
	 * @param node
	 *            the node
	 * @param line
	 *            the line to build
	 */
	private void buildLineAllocate(IRAllocate node, Line line)
	{
		line.setType("Allocate");

		if (isNotGlobal(node.getName()))
		{
			line.addDef(node.getName());
		}
		if (node.getRhs().getNodeType().equals("Load"))
		{
			IRLoad load = (IRLoad) node.getRhs();
			if (isNotGlobal(load.getName()))
			{
				line.addUse(load.getName());
			}
		}
	}

	/**
	 * Checks if a variable is not a global variable
	 * 
	 * @param var
	 *            the variable to check
	 * @return true if not global, false otherwise
	 */
	private boolean isNotGlobal(String var)
	{
		return this.locals.indexOf(var) != -1;
	}

	/**
	 * Gets all the generated method lines
	 * 
	 * @return list with the generated method lines
	 */
	public ArrayList<Line> getLines()
	{
		return lines;
	}

	/**
	 * Calculates the IN and OUT sets for all the generated method lines. It does
	 * multiple iterations, and it stops when the result of the latest two
	 * iterations is the same. In each iteration it performs a bottom-up
	 * calculation, going from the last line to the first.
	 */
	public void calculateSets()
	{
		ArrayList<BitSet> insOld;
		ArrayList<BitSet> outsOld;

		doIteration();
		
		insOld = getAllInSets();
		outsOld = getAllOutSets();

		boolean isEqual = false;

		while (!isEqual)
		{
			doIteration();

			ArrayList<BitSet> insNew = getAllInSets();
			ArrayList<BitSet> outsNew = getAllOutSets();

			isEqual = compareSetLists(insOld, insNew) && compareSetLists(outsOld, outsNew);

			insOld = insNew;
			outsOld = outsNew;
		}
	}

	/**
	 * Compares two lists of sets
	 * 
	 * @param oldSet
	 *            the first set
	 * @param newSet
	 *            the second set
	 * @return true if each set in a list equals the corresponding set in the other
	 *         list, false otherwise
	 */
	private boolean compareSetLists(ArrayList<BitSet> oldSet, ArrayList<BitSet> newSet)
	{
		for (int i = 0; i < oldSet.size(); i++)
		{
			if (!oldSet.get(i).equals(newSet.get(i)))
				return false;
		}
		return true;
	}

	/**
	 * Gets a list with all the current OUT sets from all lines
	 * 
	 * @return list with the OUT sets
	 */
	private ArrayList<BitSet> getAllOutSets()
	{
		ArrayList<BitSet> sets = new ArrayList<>();

		for (int i = 0; i < this.lines.size(); i++)
			sets.add((BitSet) this.lines.get(i).getOut().clone());
		return sets;
	}

	/**
	 * Gets a list with all the current IN sets from all lines
	 * 
	 * @return list with the IN sets
	 */
	private ArrayList<BitSet> getAllInSets()
	{
		ArrayList<BitSet> sets = new ArrayList<>();

		for (int i = 0; i < this.lines.size(); i++)
			sets.add((BitSet) this.lines.get(i).getIn().clone());
		return sets;
	}

	/**
	 * Gets a list with all the current DEF sets from all lines
	 * 
	 * @return list with the DEF sets
	 */
	private ArrayList<BitSet> getAllDefSets()
	{
		ArrayList<BitSet> sets = new ArrayList<>();

		for (int i = 0; i < this.lines.size(); i++)
			sets.add((BitSet) this.lines.get(i).getDef().clone());
		return sets;
	}

	/**
	 * Does an iteration in the IN and OUT calculus. It performs a bottom-up
	 * calculation, going from the last line to the first.
	 */
	private void doIteration()
	{
		for (int i = this.lines.size() - 1; i > -1; i--)
		{
			Line line = this.lines.get(i);

			BitSet out = calculateOut(line);
			line.setOut(out);

			BitSet in = calculateIn(line);
			line.setIn(in);
		}
	}

	/**
	 * Calculates the OUT set of a line using the formula OUT set = Union of IN sets
	 * of all successors
	 * 
	 * @param line
	 *            the line to calculate the OUT set of
	 * @return the calculated OUT set
	 */
	private BitSet calculateOut(Line line)
	{
		BitSet out = new BitSet(varToBit.size());

		for (int i = 0; i < line.getSuccessors().size(); i++)
		{
			out.or(line.getSuccessors().get(i).getIn());
		}

		return out;
	}

	/**
	 * Calculates the IN set of a line using the formula IN set = union of USE set
	 * with the difference between the OUT and DEF sets
	 * 
	 * @param line
	 *            the line to calculate the IN set of
	 * @return the calculated IN set
	 */
	private BitSet calculateIn(Line line)
	{
		BitSet out = line.getOut();
		BitSet def = line.getDef();
		BitSet use = line.getUse();
		BitSet diff = difference(out, def);

		BitSet in = new BitSet(varToBit.size());

		in.or(use);
		in.or(diff);

		return in;
	}

	/**
	 * Calculates the difference between and OUT and a DEF set
	 * 
	 * @param out
	 *            the OUT set
	 * @param def
	 *            the DEF set
	 * @return the calculated difference set
	 */
	private BitSet difference(BitSet out, BitSet def)
	{
		BitSet diff = new BitSet(varToBit.size());

		for (int i = 0; i < varToBit.size(); i++)
		{
			if (out.get(i) && !def.get(i))
				diff.set(i);
		}
		return diff;
	}

	/**
	 * Gets all the interference pairs based on the IN and OUT sets
	 * 
	 * @return list with all the interference pairs
	 */
	public ArrayList<IntPair> getAllPairs()
	{
		ArrayList<IntPair> pairs = new ArrayList<>();

		ArrayList<BitSet> ins = getAllInSets();
		for (BitSet set : ins)
			pairs.addAll(getInterferences(set));

		ArrayList<BitSet> outs = getAllOutSets();
		ArrayList<BitSet> defs = getAllDefSets();
		
		for (int i = 0; i < outs.size(); i++)
		{
			outs.get(i).or(defs.get(i));
			pairs.addAll(getInterferences(outs.get(i)));
		}
		return pairs;
	}

	/**
	 * Calculates all interferences using the IN and OUT sets, with no regard for
	 * duplicates
	 * 
	 * @param set
	 *            the set to use in order to find interferences
	 * @return a list of interference pairs
	 */
	private ArrayList<IntPair> getInterferences(BitSet set)
	{
		ArrayList<String> varList = new ArrayList<>();
		ArrayList<IntPair> pairs = new ArrayList<>();

		for (String var : this.varToBit.keySet())
		{
			int i = this.varToBit.get(var);
			if (set.get(i))
				varList.add(var);
		}
		if (varList.size() < 2)
			return pairs;

		for (int i = 0; i < varList.size(); i++)
		{
			for (int j = 0; j < varList.size(); j++)
			{
				if (i != j)
					pairs.add(new IntPair(varList.get(i), varList.get(j)));
			}
		}
		return pairs;
	}

	/**
	 * Gets all the arguments of the method
	 * 
	 * @return a list with all the method arguments
	 */
	public ArrayList<String> getAllArgs()
	{
		ArrayList<String> args = new ArrayList<>();
		Variable[] varArgs = this.node.getArgs();

		for (Variable arg : varArgs)
			args.add(arg.getVar());

		return args;
	}

	/**
	 * Gets all the local variables of the method
	 * 
	 * @return a list with all the local variables
	 */
	public ArrayList<String> getLocals()
	{
		return locals;
	}
}
