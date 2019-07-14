package yal2jvm.hlir;

import yal2jvm.Yal2jvm;

import java.util.ArrayList;

/**
 * IRNode
 */
public abstract class IRNode
{
	protected IRNode parent;
	protected ArrayList<IRNode> children;
	String nodeType;

	/**
	 * IRNode constructor
	 * @param irNode node
	 */
	public IRNode(IRNode irNode)
	{
		this.children = new ArrayList<>(irNode.getChildren());
		this.parent = irNode.parent;
		this.nodeType = irNode.getNodeType();
	}

	/**
	 * IRNode constructor without arguments
	 */
	public IRNode()
	{
		children = new ArrayList<>();
	}

	/**
	 * Adds a IRNode to the field children and makes this object its parent
	 * @param child	node to be added
	 */
	public void addChild(IRNode child)
	{
		children.add(child);
		child.setParent(this);
	}

	/**
	 * Returns the value of the field parent
	 * @return value of the field parent
	 */
	public IRNode getParent()
	{
		return parent;
	}

	/**
	 * Sets the value of the field parent to the value of the parameter parent
	 * @param parent new value for the field parent
	 */
	public void setParent(IRNode parent)
	{
		this.parent = parent;
	}

	/**
	 * Returns the value of the field children
	 * @return the value of the field children
	 */
	public ArrayList<IRNode> getChildren()
	{
		return children;
	}

	/**
	 * Sets the value of the field children to the value of the parameter children
	 * @param children	new value for the field children
	 */
	public void setChildren(ArrayList<IRNode> children)
	{
		this.children = children;
	}

	/**
	 * Gets the instructions on code generation
	 * @return instructions list
	 */
	public abstract ArrayList<String> getInstructions();

	/**
	 * Gets instructions for loading or storing more efficiently
	 * @param instruction instruction
	 * @param registerNumber register number
	 * @return instruction string
	 */
	private String getInstructionLoadOrStoreInstructionMoreEfficient(String instruction, int registerNumber)
	{
		if (registerNumber < 4)
			return instruction + "_" + registerNumber;
		else
			return instruction + " " + registerNumber;
	}

	/**
	 * Gets instructions for loading int from register to stack
	 * @param registerNumber register number
	 * @return instruction string
	 */
	String getInstructionToLoadIntFromRegisterToStack(int registerNumber)
	{
		return getInstructionLoadOrStoreInstructionMoreEfficient("iload", registerNumber);
	}

	/**
	 * Gets instructions for storing int in register
	 * @param registerNumber register number
	 * @return instruction string
	 */
	String getInstructionToStoreIntInRegister(int registerNumber)
	{
		return getInstructionLoadOrStoreInstructionMoreEfficient("istore", registerNumber);
	}

	/**
	 * Gets instructions for loading an array from register to stack
	 * @param registerNumber register number
	 * @return instruction string
	 */
	String getInstructionToLoadArrayFromRegisterToStack(int registerNumber)
	{
		return getInstructionLoadOrStoreInstructionMoreEfficient("aload", registerNumber);
	}

	/**
	 * Gets instructions for storing an array in a register
	 * @param registerNumber register number
	 * @return instruction string
	 */
	String getInstructionToStoreArrayInRegister(int registerNumber)
	{
		return getInstructionLoadOrStoreInstructionMoreEfficient("astore", registerNumber);
	}

	/**
	 * Gets instructions for loading a global to the stack
	 * @param var global variable
	 * @return instruction string
	 */
	String getInstructionToLoadGlobalToStack(Variable var)
	{
		String varType = var.getType() == Type.INTEGER ? "I" : "[I";
		return "getstatic " + Yal2jvm.moduleName + "/" + var.getVar() + " " + varType;
	}

	/**
	 * Gets instructions for loading a global to the stack
	 * @param var global variable
	 * @return instruction string
	 */
	String getInstructionToStoreGlobal(Variable var)
	{
		String varType = var.getType() == Type.INTEGER ? "I" : "[I";
		return "putstatic " + Yal2jvm.moduleName + "/" + var.getVar() + " " + varType;
	}

	/**
	 * Returns the value of the field nodeType
	 * @return	value of the field nodeType
	 */
	@Override
	public String toString()
	{
		return this.nodeType;
	}

	/**
	 * Finds the ancestor with a given type
	 * @param nodeType ancestor node type
	 * @return ancestor node
	 */
	public IRNode findParent(String nodeType)
	{
		IRNode res;
		IRNode par = this.parent;
		while (true)
		{
			if (par.toString().equals(nodeType))
			{
				res = par;
				break;
			} else
			{
				par = par.getParent();
				if (par == null)
				{
					return null;
				}
			}
		}

		return res;
	}

	/**
	 * Gets a var node if it exists
	 * @param varName variable name
	 * @return variable node
	 */
	IRNode getVarIfExists(String varName)
	{
		IRModule module = (IRModule) findParent("Module");
		IRGlobal irGlobal = module.getGlobal(varName);
		if (irGlobal != null)
			return irGlobal;

		IRMethod method = (IRMethod) findParent("Method");
		int register = method.getArgumentRegister(varName);
		if (register != -1)
			return new IRArgument(register);

		ArrayList<IRNode> children = method.getChildren();
		for (IRNode aChildren : children) {
			if (aChildren.toString().equals("Allocate")) {
				IRAllocate alloc = (IRAllocate) aChildren;
				if (alloc.getName().equals(varName)) {
					alloc.getRegister();
					return alloc;
				}
			}
		}

		return null;
	}

	/**
	 * Sets a local array element by an IRNode
	 * @param index index node
	 * @param register register number
	 * @param value value node
	 * @return the list of instructions to set Array Element
	 */
	ArrayList<String> setLocalArrayElementByIRNode(IRNode index, int register, IRNode value)
	{
		String loadArrayRefInstruction = getInstructionToLoadArrayFromRegisterToStack(register);
		return setArrayElement(index.getInstructions(), loadArrayRefInstruction, value);
	}

	/**
	 * Sets a global array element by an IRNode
	 * @param index index node
	 * @param var variable
	 * @param value value
	 * @return instructions list
	 */
	ArrayList<String> setGlobalArrayElementByIRNode(IRNode index, Variable var, IRNode value)
	{
		String loadArrayRefInstruction = getInstructionToLoadGlobalToStack(var);
		return setArrayElement(index.getInstructions(), loadArrayRefInstruction, value);
	}

	/**
	 * Sets an array element
	 * @param indexInstructions index instructions
	 * @param loadArrayRefInstruction instruction for loading an array ref
	 * @param value value node
	 * @return instructions list
	 */
	private ArrayList<String> setArrayElement(ArrayList<String> indexInstructions, String loadArrayRefInstruction,
											  IRNode value)
	{
		ArrayList<String> inst = new ArrayList<>();

		inst.add(loadArrayRefInstruction);
		inst.addAll(indexInstructions);
		inst.addAll(value.getInstructions());
		inst.add("iastore");

		return inst;
	}

	/**
	 * get the global variable jvm code
	 * @param name variable name
	 * @param module module node
	 * @return the global variable jvm code
	 */
	String getGlobalVariableGetCode(String name, IRModule module)
	{
		IRGlobal global = module.getGlobal(name);
		if (global == null)
		{
			System.out.println("Internal error! The program will be closed.");
			System.exit(-1);
		}

		String in = "getstatic " + module.getName() + "/" + global.getName() + " ";
		in += global.getType() == Type.ARRAY ? "[I" : "I";

		return in;
	}

	/**
	 * get the global variable jvm code
	 * @param name variable name
	 * @param method method node
	 * @return the global variable jvm code
	 */
	String getGlobalVariableGetCodeByIRMethod(String name, IRMethod method)
	{
		IRModule module = ((IRModule) method.getParent());
		return getGlobalVariableGetCode(name, module);
	}

	/**
	 * gets the list of instructions to Set All Array Elements
	 * @param arrayRefJVMCode reference to the array
	 * @param valueJVMCode value to set to set in the array element
	 * @return list of instructions to Set All Array Elements
	 */
	ArrayList<String> getCodeForSetAllArrayElements(String arrayRefJVMCode, ArrayList<String> valueJVMCode)
	{
		ArrayList<String> inst = new ArrayList<>();

		inst.add(arrayRefJVMCode);
		inst.add("arraylength");
		inst.add("init:");
		inst.add("iconst_1");
		inst.add("isub");
		inst.add("dup");
		inst.add("dup");
		inst.add("iflt end");
		inst.add(arrayRefJVMCode);
		inst.add("swap");
		inst.addAll(valueJVMCode);
		inst.add("iastore");
		inst.add("goto init");
		inst.add("end:");

		return inst;
	}

	/**
	 * Returns the value of the field nodeType
	 * @return	value of the field nodeType
	 */
	public String getNodeType()
	{
		return nodeType;
	}

	/**
	 * Sets the value of the field nodeType to the value of the parameter nodeType
	 * @param nodeType new value for the field nodeType
	 */
	public void setNodeType(String nodeType)
	{
		this.nodeType = nodeType;
	}

	/**
	 * get the value of a const variable by name and index (if applicable)
	 * @param name name of the variable to get value
	 * @param index index of array access
	 * @return the value of the variable if constant
	 */
	public String getVarNameForConstantName(String name, IRNode index)
	{
		String varName = name; // not array access, so integer
		if (index != null && index instanceof IRConstant) // array access then
			varName = name + "-" + ((IRConstant) index).getValue();

		return varName;
	}
}
