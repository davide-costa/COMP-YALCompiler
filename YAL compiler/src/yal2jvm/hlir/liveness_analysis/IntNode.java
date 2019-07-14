package yal2jvm.hlir.liveness_analysis;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class that represents a node of an interference graph
 */
public class IntNode implements Serializable
{
	private String name;
	private ArrayList<IntNode> interferences;
	private int requiredRegister = -1;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            name of the variable
	 */
	public IntNode(String name)
	{
		this.name = name;
		this.interferences = new ArrayList<>();
	}

	/**
	 * Adds an interference to this node
	 * 
	 * @param node
	 *            the node to add the interference to
	 */
	public void addInterference(IntNode node)
	{
		if (node.getName().equals(this.name))
			return;
		if (this.interferences.indexOf(node) == -1)
			this.interferences.add(node);
	}

	/**
	 * Removes an interference with a node
	 * 
	 * @param node
	 *            the node to remove the interference with
	 */
	public void removeInterference(IntNode node)
	{
		this.interferences.remove(node);
	}

	/**
	 * Checks this node for equality with another. Two nodes are equal if they
	 * represent the same variable
	 * 
	 * @param o
	 *            the node to compare with
	 * @return true if they are equal, false otherwise
	 */
	@Override
	public boolean equals(Object o)
	{
		return this.name.equals(((IntNode) o).getName());
	}

	/**
	 * Gets the name of the variable represented by this node
	 * 
	 * @return the variable name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the name of the variable represented by this node
	 * 
	 * @param name
	 *            the variable name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Gets all the interferences of this node
	 * 
	 * @return a list with the nodes that interfere with this one
	 */
	public ArrayList<IntNode> getInterferences()
	{
		return interferences;
	}

	/**
	 * Gets the indegree of the node, that is, its number of edges (interferences
	 * with other variables)
	 * 
	 * @return
	 */
	public int indegree()
	{
		return this.interferences.size();
	}

	/**
	 * Gets a String representation of this node, showing all its interferences.
	 * 
	 * @return a String representation of the node
	 */
	@Override
	public String toString()
	{
		String s = this.name + " --> [";
		for (int i = 0; i < this.interferences.size(); i++)
			s += this.interferences.get(i).getName() + ", ";
		if (this.interferences.size() > 0)
			s = s.substring(0, s.length() - 2);
		s += "]";
		if (this.requiredRegister != -1)
			s += " Required reg: " + this.requiredRegister;
		return s;
	}

	/**
	 * Gets the required register of the variable represented by this node. If there
	 * is no required register, it returns -1.
	 * 
	 * @return
	 */
	public int getRequiredRegister()
	{
		return requiredRegister;
	}

	/**
	 * Sets a required register for the variable represented by this node
	 * 
	 * @param requiredRegister
	 *            the required register number
	 */
	public void setRequiredRegister(int requiredRegister)
	{
		this.requiredRegister = requiredRegister;
	}
}
