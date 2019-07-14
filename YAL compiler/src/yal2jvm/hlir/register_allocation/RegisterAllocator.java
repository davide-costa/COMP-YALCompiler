package yal2jvm.hlir.register_allocation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import yal2jvm.Yal2jvm;
import yal2jvm.hlir.liveness_analysis.IntGraph;

/**
 * Class responsible for register allocation, receiving the hashMap with functions names and the respective interferences graphs.
 * Using GraphColoring class finds the suitable registers for each variable
 */
public class RegisterAllocator
{
	private HashMap<String, IntGraph> intGraphs;
	private HashMap<String, HashMap<String, Integer>> allocatedRegisterByMethodName = new HashMap<>();

	/**
	 * Constructor to RegisterAllocator
	 * @param intGraphs hashMap with functions names and the respective interferences graphs
	 */
	public RegisterAllocator(HashMap<String, IntGraph> intGraphs)
	{
		this.intGraphs = intGraphs;
	}

	/**
	 * Method responsible for register allocation, receiving the hashMap with functions names and the respective interferences graphs.
	 * Using GraphColoring class finds the suitable registers for each variable
	 * Outputs an error if could not allocate.
	 * @param numberRegisters number of registers allowed to use in register allocation
	 * @return true if could allocate all methods with the specified number od registers, false otherwise
	 */
	public boolean allocate(int numberRegisters)
	{
		if (Yal2jvm.VERBOSE)
		{
			System.out.println("Doing register allocation for each method\n");
		}

		Iterator it = intGraphs.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry pair = (Map.Entry) it.next();
			String methodName = (String) pair.getKey();
			GraphColoring graphColoring = new GraphColoring((IntGraph) pair.getValue(), numberRegisters);
			if (graphColoring.colorGraph() == false)
			{
				System.out.println("Error allocating registers to method " + methodName + ".");
				int numRegisterThatAllowToAllocate = findNumberOfRegisterThatAllowToAllocate(graphColoring,
						numberRegisters);
				System.out.println(
						"Number of registers must be equal or higher than " + numRegisterThatAllowToAllocate + ".");
				return false;
			} else
			{
				if (Yal2jvm.VERBOSE)
					System.out.println("Successfull register allocation with a maximum of " + numberRegisters
							+ " registers for method " + methodName);
				allocatedRegisterByMethodName.put(methodName, graphColoring.getVarNameToRegisterNumber());
			}
		}

		return true;
	}

	/**
	 * finds the less number of registers with which an allocate with the used algorithm in GraphColoring is possible
	 * @param graphColoring the GraphColoring object used to find the suitable registers
	 * @param currNumberOfRegisters curr number of registers, the number of registers specified in class constructor
	 * @return the number of registers with which the allocation was possible
	 */
	private int findNumberOfRegisterThatAllowToAllocate(GraphColoring graphColoring, int currNumberOfRegisters)
	{
		do
		{
			currNumberOfRegisters++;
			graphColoring.setNumRegisters(currNumberOfRegisters);
		} while (graphColoring.colorGraph() == false);

		return currNumberOfRegisters;
	}

	/**
	 * Returns the value of the field allocatedRegisterByMethodName
	 * @return value of the field allocatedRegisterByMethodName
	 */
	public HashMap<String, HashMap<String, Integer>> getAllocatedRegisterByMethodName()
	{
		return allocatedRegisterByMethodName;
	}

}
