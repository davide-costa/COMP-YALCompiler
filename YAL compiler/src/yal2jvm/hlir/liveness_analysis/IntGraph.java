package yal2jvm.hlir.liveness_analysis;

import java.io.*;
import java.util.ArrayList;

/**
 * Class that represents an interference graph of the variables from a method.
 * It is used to bridge the gap between the dataflow analysis and the register
 * allocation.
 */
public class IntGraph implements Serializable
{
	private static final long serialVersionUID = -567491277226468354L;
	private ArrayList<IntNode> nodes;

	/**
	 * Copy constructor of the IntGraph, it creates a new IntGraph based on the
	 * graph passed by argument. As the graph has a list of nodes (IntNode objects)
	 * that have themselves a list of nodes (we had problems with recursivity in
	 * constructors) and the only solution we found was to to make a copy using
	 * serialization, method getGraphCopy.
	 * 
	 * @param graph
	 *            based on the new IntGraph will be created
	 */
	public IntGraph(IntGraph graph)
	{
		nodes = getGraphCopy(graph).getNodes();
	}

	/**
	 * Creates a new empty graph
	 */
	public IntGraph()
	{
		this.nodes = new ArrayList<>();
	}

	/**
	 * Adds an interference between two variables, and creates the variables (nodes
	 * of the graph) if they don't yet exist. It is idempotent.
	 * 
	 * @param var1
	 *            name of the first variable
	 * @param var2
	 *            name of the second variable
	 */
	public void addInterference(String var1, String var2)
	{
		IntNode n1 = findNode(var1);
		IntNode n2 = findNode(var2);

		n1.addInterference(n2);
		n2.addInterference(n1);
	}

	/**
	 * Adds a new node to the graph
	 * 
	 * @param node
	 *            the node to be added
	 */
	public void addNode(IntNode node)
	{
		ArrayList<IntNode> graphNodeInterferences = node.getInterferences();
		for (IntNode graphNodeToInterference : graphNodeInterferences)
		{
			graphNodeToInterference.addInterference(node);
		}

		nodes.add(node);
	}

	/**
	 * Removes a node from the graph
	 * 
	 * @param node
	 *            node to be removed
	 */
	public void removeNode(IntNode node)
	{
		int nodeIndex = nodes.indexOf(node);
		IntNode graphNodeToRemove = nodes.remove(nodeIndex);

		ArrayList<IntNode> graphNodeToRemoveInterferences = graphNodeToRemove.getInterferences();
		for (IntNode graphNodeToRemoveInterference : graphNodeToRemoveInterferences)
		{
			graphNodeToRemoveInterference.removeInterference(graphNodeToRemove);
		}
	}

	/**
	 * Adds a new variable to the graph, creating a node for it. It is idempotent.
	 * 
	 * @param var
	 *            the name of the variable
	 */
	public void addVariable(String var)
	{
		for (IntNode n : this.nodes)
		{
			if (n.getName() == var)
				return;
		}
		IntNode node = new IntNode(var);
		this.nodes.add(node);
	}

	/**
	 * Finds the node with the specified variable name. If it doesn't exist, it is
	 * created
	 * 
	 * @param var
	 *            the variable name
	 * @return the node of the variable
	 */
	private IntNode findNode(String var)
	{
		for (IntNode n : this.nodes)
		{
			if (n.getName() == var)
				return n;
		}
		IntNode node = new IntNode(var);
		this.nodes.add(node);
		return node;
	}

	/**
	 * Gets all the nodes
	 * 
	 * @return list with all the nodes
	 */
	public ArrayList<IntNode> getNodes()
	{
		return nodes;
	}

	/**
	 * Build a string of the graph, containing, in each line, a variable and its
	 * interferences
	 * 
	 * @return a String representation of the graph
	 */
	@Override
	public String toString()
	{
		String s = "";
		for (int i = 0; i < this.nodes.size(); i++)
			s += this.nodes.get(i).toString() + "\n";
		return s;
	}

	/**
	 * Sets the mandatory registers for the method arguments. Argument 1 will have
	 * register 0, argument 2 register 1, and so on. This information must be
	 * assigned right now in order to prevent misassigns during the register
	 * allocation phase.
	 * 
	 * @param args
	 * @return
	 */
	public void setRequiredRegisters(ArrayList<String> args)
	{
		for (int i = 0; i < args.size(); i++)
		{
			for (IntNode node : this.nodes)
			{
				if (node.getName().equals(args.get(i)))
					node.setRequiredRegister(i);
			}
		}
	}

	/**
	 * Uses serialization to create a deep copy of the graph. It saves the graph
	 * object to a file tempdata.ser and read it again. This method a retry count,
	 * in order to prevent common errors deleting temp file or with stream fail.
	 * 
	 * @param graph
	 * @return a deep copy of the graph passed in by argument
	 */
	private IntGraph getGraphCopy(IntGraph graph)
	{
		int maxAttempts = 10;
		int counter = 0;
		boolean fileSuccessfullyDeleted = false;
		while (counter < maxAttempts && fileSuccessfullyDeleted == false)
		{
			try
			{
				// write data
				FileOutputStream fos = new FileOutputStream("tempdata.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(graph);
				oos.close();

				// read data
				FileInputStream fis = new FileInputStream("tempdata.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				IntGraph graphRead = (IntGraph) ois.readObject();
				ois.close();

				// delete temp file
				fileSuccessfullyDeleted = new File("tempdata.ser").delete();

				return graphRead;
			} catch (Exception ex)
			{
				counter++;
			}
		}

		System.out.println("Exception thrown during IntGraph copy");
		System.exit(-1);
		return null;
	}
}
