package yal2jvm.hlir.register_allocation;

import yal2jvm.hlir.liveness_analysis.IntGraph;
import yal2jvm.hlir.liveness_analysis.IntNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *	Class responsible for graph coloring, allowing to allocate registers.
 */
public class GraphColoring
{
	private IntGraph graph;
	private int numRegisters;
	private List<Integer> registers;
	private Stack<IntNode> nodesToColorStack = new Stack<>();
	private HashMap<String, Integer> varNameToRegisterNumber = new HashMap<>();

	/**
	 * Constructor for GraphColoring.
	 * @param graph interferences graph
	 * @param numRegisters number of registers that can be allocated
	 */
	public GraphColoring(IntGraph graph, int numRegisters)
	{
		this.graph = graph;
		this.numRegisters = numRegisters;
		this.registers = IntStream.rangeClosed(0, numRegisters - 1).boxed().collect(Collectors.toList());
	}

	/**
	 * set number of registers
	 * @param numRegisters new number of registers
	 */
	public void setNumRegisters(int numRegisters)
	{
		this.numRegisters = numRegisters;
		this.registers = IntStream.rangeClosed(0, numRegisters - 1).boxed().collect(Collectors.toList());
	}

	/**
	 * Returns the value of the field varNameToRegisterNumber
	 * @return	value of the field varnameToRegisterNumber
	 */
	public HashMap<String, Integer> getVarNameToRegisterNumber()
	{
		return varNameToRegisterNumber;
	}

	/**
	 * build the stack of nodes to color from the interferences graph
	 * @return boolean indicating the success of the operation, false otherwise
	 */
	private boolean buildStackOfNodesToColor()
	{
		ArrayList<IntNode> listNodesOriginalGraph = graph.getNodes();
		IntGraph graphCopy = new IntGraph(graph);
		ArrayList<IntNode> listNodesGraphCopy = graphCopy.getNodes();
		int lastRegisterNumber = 0;
		for (int i = 0; i < listNodesGraphCopy.size(); i++)
		{
			IntNode node = listNodesGraphCopy.get(i);
			int nodeRequiredRegister = node.getRequiredRegister();
			if (nodeRequiredRegister > lastRegisterNumber)
				lastRegisterNumber = nodeRequiredRegister;
			if (node.indegree() < numRegisters && nodeRequiredRegister == -1) // indegree less than numRegisters and not
																				// parameter
			{
				nodesToColorStack.push(listNodesOriginalGraph.get(listNodesOriginalGraph.indexOf(node)));
				graphCopy.removeNode(node);
				i = -1;
			}
		}

		if (listNodesGraphCopy.size() > lastRegisterNumber + 1) // lastRegisterNumber + 1 = number of registers
			return false;

		int expectedValue = lastRegisterNumber;
		for (int i = 0; i < listNodesGraphCopy.size(); i++)
		{
			IntNode node = listNodesGraphCopy.get(i);
			if (node.indegree() < numRegisters || node.getRequiredRegister() == expectedValue) // indegree less than
																								// numRegisters
			{
				expectedValue--;
				nodesToColorStack.push(listNodesOriginalGraph.get(listNodesOriginalGraph.indexOf(node)));
				graphCopy.removeNode(node);
				i = -1;
			}
		}

		return listNodesGraphCopy.size() == 0;
	}

	/**
	 * colors graph using "Heuristics Solution for Graph Coloring" algorithm
	 * @return
	 */
	public boolean colorGraph()
	{
		if (buildStackOfNodesToColor() == false)
			return false;

		while (nodesToColorStack.empty() == false)
		{
			IntNode node = nodesToColorStack.pop();

			ArrayList<Integer> usedRegisters = new ArrayList<>();
			ArrayList<IntNode> nodeInterferences = node.getInterferences();
			for (IntNode interference : nodeInterferences)
			{
				Integer registerNumber = varNameToRegisterNumber.get(interference.getName());
				if (registerNumber != null)
					usedRegisters.add(registerNumber);
			}

			Integer register = findFirstUnusedRegisterThatMatchesRequired(usedRegisters, node.getRequiredRegister());
			if (register == null)
			{
				System.out.println("Internal error coloring graph - colorGraph of class GraphColoring.");
				System.exit(-1);
			}

			varNameToRegisterNumber.put(node.getName(), register);
		}

		return true;
	}

	/**
	 * find first unused register that matches the required register (for arguments), or that has not a specific requirement
	 * @param usedRegisters
	 * @return
	 */
	private Integer findFirstUnusedRegisterThatMatchesRequired(ArrayList<Integer> usedRegisters, int requiredRegister)
	{
		for (Integer register : registers)
		{
			// if the register is not occupied and this variable does not require a specific
			// register (requiredRegister == -1)
			// or the specific register that requires is the one being given
			if (usedRegisters.contains(register) == false
					&& (requiredRegister == -1 || register.intValue() == requiredRegister))
				return register;
		}

		return null;
	}

}