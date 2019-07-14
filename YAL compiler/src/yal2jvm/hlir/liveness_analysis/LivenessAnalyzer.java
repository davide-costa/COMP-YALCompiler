package yal2jvm.hlir.liveness_analysis;

import java.util.ArrayList;
import java.util.HashMap;

import yal2jvm.hlir.IRMethod;
import yal2jvm.hlir.IRModule;
import yal2jvm.hlir.IRNode;

/**
 * Class to perform liveness analysis of an entire module, producing an
 * interference graph for each of the module's methods as a result
 */
public class LivenessAnalyzer
{
	private IRModule ir;
	private HashMap<String, IntGraph> intGraphs;

	/**
	 * Constructor
	 * 
	 * @param ir
	 *            root node of the HLIR
	 */
	public LivenessAnalyzer(IRModule ir)
	{
		this.ir = ir;
		this.intGraphs = new HashMap<>();
	}

	/**
	 * Performs a liveness analysis for each of the module's methods
	 */
	public void analyze()
	{
		ArrayList<IRNode> children = ir.getChildren();
		for (IRNode n : children)
		{
			if (n.getNodeType().equals("Method"))
			{
				IRMethod method = (IRMethod) n;
				SetBuilder met = new SetBuilder(method);
				MethodAnalyzer analyzer = new MethodAnalyzer(met);
				analyzer.analyze();
				this.intGraphs.put(method.getName(), analyzer.getGraph());
			}
		}
	}

	/**
	 * Retrieves an HashMap containing, for each method, the interference graph of
	 * its variables
	 * 
	 * @return the HashMap with the interference graphs
	 */
	public HashMap<String, IntGraph> getInterferenceGraphs()
	{
		return intGraphs;
	}
}
