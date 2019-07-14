package yal2jvm.hlir.liveness_analysis;

import java.util.ArrayList;

import yal2jvm.Yal2jvm;

/**
 * Wrapper class for the dataflow and liveness analysis of a method using the
 * SetBuilder class
 */
public class MethodAnalyzer
{
	private SetBuilder method;
	private String methodName;

	/**
	 * Constructor
	 * 
	 * @param method
	 *            the method to analyze
	 */
	public MethodAnalyzer(SetBuilder method)
	{
		this.method = method;
		this.methodName = method.getName();
	}

	/**
	 * Analyzes the method and fetches the resultant interference graph internally
	 */
	public void analyze()
	{
		method.getAllVars();
		method.buildAllLines();
		method.calculateSets();
		getGraph();
		printResults();
	}

	/**
	 * Prints the results of the liveness analysis
	 */
	private void printResults()
	{
		if (!Yal2jvm.VERBOSE)
			return;

		System.out.println("Liveness analysis of method " + methodName + ":\n");
		ArrayList<Line> lines = method.getLines();
		System.out.println("Local vars: " + method.getLocals() + "\n");
		for (Line l : lines)
			System.out.println(l);
		System.out.println("\nInterferences and mandatory registers:");
		System.out.println(getGraph().toString());
	}

	/**
	 * Gets the interference graph for this method
	 * 
	 * @return the interference graph
	 */
	public IntGraph getGraph()
	{
		ArrayList<IntPair> interferences = method.getAllPairs();

		IntGraph graph = new IntGraph();
		for (IntPair pair : interferences)
			graph.addInterference(pair.getVar1(), pair.getVar2());

		ArrayList<String> locals = method.getLocals();
		for (String local : locals)
			graph.addVariable(local);

		ArrayList<String> args = method.getAllArgs();
		graph.setRequiredRegisters(args);

		return graph;
	}
}
