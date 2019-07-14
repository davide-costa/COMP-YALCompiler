package yal2jvm.hlir;

import java.util.ArrayList;

/**
 *	Class responsible for the intermediate representation for lables. Class that extend IRNode class.
 */
public class IRLabel extends IRNode
{
	private String label;

	/**
	 * IRLabel constructor
	 * @param label name of the label to create
	 */
	public IRLabel(String label)
	{
		this.label = label;
		this.setNodeType("Label");
	}

	/**
	 * Gets the instructions for code generation
	 * @return instructions list
	 */
	@Override
	public ArrayList<String> getInstructions()
	{
		ArrayList<String> inst = new ArrayList<>();
		inst.add(label + ":");
		return inst;
	}

	/**
	 * Returns the value of the field Label
	 * @return	value of the field Label
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * Sets the value of the field Label to the value of the parameter label
	 * @param label	new value for the field Label
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}
}
