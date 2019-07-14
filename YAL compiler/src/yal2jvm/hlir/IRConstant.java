package yal2jvm.hlir;

import java.util.ArrayList;

/**
 *	Class responsible for the intermediate representation for constants. Class that extend IRNode class.
 */
public class IRConstant extends IRNode
{
	private String value;

	/**
	 * Constructor for the class IRConstant
	 * 
	 * @param irConstant
	 */
	public IRConstant(IRConstant irConstant)
	{
		super(irConstant);
		this.value = new String(irConstant.getValue());
	}

	/**
	 * Constructor for IRConstant
	 * @param value the value of the constant
	 */
	public IRConstant(String value)
	{
		this.value = value;
		this.setNodeType("Constant");
	}

	/**
	 * Gets the instructions for code generation
	 * @return instructions list
	 */
	@Override
	public ArrayList<String> getInstructions()
	{
		ArrayList<String> inst = new ArrayList<>();
		try
		{
			int integer = Integer.parseInt(value);
			inst.add(getLoadConstantInstruction(integer));
		} catch (NumberFormatException nfe) // if value is string type
		{
			inst.add("ldc " + value);
		}

		return inst;
	}

	/**
	 * gets the most efficient jvm code for the constant load
	 * @param value  value of the constant
	 * @return the most efficient jvm code for the constant load
	 */
	public static String getLoadConstantInstruction(int value)
	{
		if (value <= 5 && value >= -1)
		{
			if (value == -1)
			{
				return "iconst_m1";
			} else
			{
				return "iconst_" + value;
			}
		} else
		{
			if (value <= 32767 && value >= -32768)
			{
				if (value <= 127 && value >= -128)
				{
					return "bipush " + value;
				} else
				{
					return "sipush " + value;
				}
			} else
			{
				return "ldc " + value;
			}
		}
	}

	/**
	 * Returns the value of the field value
	 * @return	value of the field value
	 */
	public String getValue()
	{
		return value;
	}

	/**
	 * Sets the value of field value to the value of the parameter value
	 * @param value new value of the field value
	 */
	public void setValue(String value)
	{
		this.value = value;
	}

	/**
	 * clone the object
	 * @return IRConstant instance clone
	 */
	public Object clone()
	{
		return new IRConstant(this);
	}
}
