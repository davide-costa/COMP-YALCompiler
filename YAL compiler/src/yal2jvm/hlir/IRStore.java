package yal2jvm.hlir;

import java.util.ArrayList;

/**
 * IRStore
 */
public abstract class IRStore extends IRNode
{
	protected String name;
	boolean arrayAccess = false;
	protected IRNode index = null;
	private int register;

	/**
	 * Gets instruction for storing
	 * @param arrayAccess true if it is an array access
	 * @param index index node
	 * @param value value node
	 * @return instructions list
	 */
	ArrayList<String> getInstForStoring(boolean arrayAccess, IRNode index, IRNode value)
	{
		IRMethod method = (IRMethod) parent;

		// check if it is one of the method's arguments
		register = method.getArgumentRegister(name);

		// if not, check if storage variable exists, and if so get its register
		if (register == -1)
		{
			IRAllocate var = ((IRMethod) parent).getVarDeclaredUntilThis(name, this);
			if (var != null)
				register = var.getRegister();
		}

		// code for check global
		IRModule module = (IRModule) findParent("Module");
		if (register == -1)
		{
			IRGlobal global = module.getGlobal(name);
			if (global != null)
			{
				addVariableToConstIfAppropriated(value, method);
				return getInstForStoringGlobalVariable(index, value, module, global);
			}
		}

		// if storage variable does not exist (locally or globally), allocate it
		if (register == -1)
		{
			IRAllocate irAllocate = new IRAllocate(name, new Variable("0", Type.INTEGER));
			method.addNewChildAfterChild(this, irAllocate);
			register = irAllocate.getRegister();
		}

		addVariableToConstIfAppropriated(value, method);
		return getInstForStoringLocalVariable(arrayAccess, index, value);
	}

	/**
	 * Adds a varaible to const if is appropriated
	 * @param value value node
	 * @param method method node
	 */
	private void addVariableToConstIfAppropriated(IRNode value, IRMethod method)
	{
		if (value instanceof IRArith)
		{
			String valueString = ((IRArith) value).getStringValueIfBothConstant();
			if (valueString != null)
			{
				String varName = getVarNameForConstantName(name, index);
				method.addToConstVarNameToConstValue(varName, new IRConstant(valueString));
				return;
			}
		}

		method.removeFromConstVarNameToConstValue(name);
	}

	/**
	 * Gets the instructions for storing a local variable
	 * @param arrayAccess true if it is an array access
	 * @param index index node
	 * @param value value node
	 * @return instructions list
	 */
	private ArrayList<String> getInstForStoringLocalVariable(boolean arrayAccess, IRNode index, IRNode value)
	{
		ArrayList<String> inst = new ArrayList<>();
		if (arrayAccess)
		{
			inst.addAll(setLocalArrayElementByIRNode(index, register, value));
		} else
		{
			inst.addAll(value.getInstructions());
			if (value instanceof IRCall && ((IRCall) value).getType().equals(Type.ARRAY))
				inst.add(getInstructionToStoreArrayInRegister(register));
			else
				inst.add(getInstructionToStoreIntInRegister(register));
		}

		return inst;
	}

	/**
	 * Gets instructions for storing a global variable
	 * @param index index node
	 * @param value value node
	 * @param module module node
	 * @param global global node
	 * @return instructions list
	 */
	private ArrayList<String> getInstForStoringGlobalVariable(IRNode index, IRNode value, IRModule module,
			IRGlobal global)
	{
		ArrayList<String> inst = new ArrayList<>();
		if (global.getType() == Type.ARRAY)
			inst.addAll(setGlobalArrayElementByIRNode(index, new Variable(name, Type.ARRAY), value));
		else
		{
			// type = integer or type = variable
			inst.addAll(value.getInstructions());
			String instruction = "putstatic " + module.getName() + "/" + name + " I";
			inst.add(instruction);
		}

		return inst;
	}

	/**
	 * Returns the value of the field name
	 * @return value of the field name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the name of the field name to the value of the parameter name
	 * @param name new value for the field name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

}
