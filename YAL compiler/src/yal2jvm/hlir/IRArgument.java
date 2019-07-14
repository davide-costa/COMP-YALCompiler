package yal2jvm.hlir;

import java.util.ArrayList;

/**
 *	Class responsible for the intermediate representation for method arguments. Class that extend IRNode class
 */
public class IRArgument extends IRNode
{
	private int register;

	/**
	 * Constructor for IRArgument
	 * @param register the register where argument is placed
	 */
	public IRArgument(int register)
	{
		this.register = register;
	}

	/**
	 * Gets the instructions for code generation
	 * @return instructions list
	 */
	@Override
	public ArrayList<String> getInstructions()
	{
		return null;
	}

	/**
	 * Returns the value of the field register
	 * @return	value of the field register
	 */
	public int getRegister()
	{
		return register;
	}
}
