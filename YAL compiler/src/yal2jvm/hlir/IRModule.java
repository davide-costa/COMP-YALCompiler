package yal2jvm.hlir;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * IRModule
 */
public class IRModule extends IRNode
{
	private String name;
	private int currLabelNumber = 1;

	/**
	 * IRModule constructor
	 * @param name module name
	 */
	IRModule(String name)
	{
		super();
		this.setName(name);
		this.setNodeType("Module");
	}

	/**
	 * Gets the instructions on code generation
	 * @return instructions list
	 */
	@Override
	public ArrayList<String> getInstructions()
	{
		ArrayList<String> inst = new ArrayList<>();

		String inst1 = ".class public static " + name;
		String inst2 = ".super java/lang/Object";

		inst.add(inst1);
		inst.add(inst2);
		inst.add("\n");

		for (int i = 0; i < getChildren().size(); i++)
		{
			if (getChildren().get(i).toString().equals("Method"))
				inst.add("\n");
			inst.addAll(getChildren().get(i).getInstructions());
		}

		return inst;
	}

	/**
	 * Returns the value of the field name
	 * @return	value of the field name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the value of the field name to the value of the parameter name
	 * @param name	new value for the field name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Returns the value of the field currLabelNumber incremented by one
	 * @return	the value of the field currLabelNumber plus one
	 */
	public int getAndIncrementCurrLabelNumber()
	{
		return currLabelNumber++;
	}

	/**
	 * Gets global variable
	 * @param name variable name
	 * @return global node
	 */
	public IRGlobal getGlobal(String name)
	{
		for (IRNode aChildren : children) {
			if (aChildren.toString().equals("Global")) {
				IRGlobal global = ((IRGlobal) aChildren);
				if (global.getName().equals(name))
					return global;
			}
		}
		return null;
	}

	/**
	 * Gets a child method
	 * @param name method name
	 * @return method node
	 */
	public IRMethod getChildMethod(String name)
	{
		for (IRNode child : children) {
			if (child instanceof IRMethod) {
				if (((IRMethod) child).getName().equals(name))
					return ((IRMethod) child);
			}
		}
		return null;
	}

	/**
	 * Gets all globals
	 * @return list of globals names
	 */
	public TreeSet<String> getAllGlobals()
	{
		TreeSet<String> globals = new TreeSet<>();
		for (IRNode i : children)
		{
			if (i.getNodeType().equals("Global"))
			{
				IRGlobal gl = (IRGlobal) i;
				globals.add(gl.getName());
			}
		}
		return globals;
	}
}
