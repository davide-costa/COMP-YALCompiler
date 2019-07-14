package yal2jvm.hlir;

import java.util.ArrayList;

/**
 *	Class responsible for the intermediate representation for loads. Class that extend IRNode class.
 */
public class IRLoad extends IRNode
{
	private String name;
	private int register = -1;
	private Type type;
	private IRNode index = null;
	private boolean arraySizeAccess;
	private String loadedConstantValue = null;

	/**
	 * Constructor for IRLoad
	 * @param name of the variable  to load
	 */
	private IRLoad(String name)
	{
		this.name = name;
		this.setNodeType("Load");
	}

	/**
	 * Constructor for IRLoad
	 * @param name of the variable  to load
	 * @param type of the variable  to load
	 */
	public IRLoad(String name, Type type)
	{
		this(name);
		this.type = type;
	}

	/**
	 * Constructor for IRLoad
	 * @param value of the variable to load, type INTEGER
	 */
	public IRLoad(Variable value)
	{
		this(value.getVar());
		this.type = Type.INTEGER; // assumes type is integer and changes if needed
		if (value.isSizeAccess())
		{
			arraySizeAccess = true;
			this.type = Type.ARRAY;
		}
	}

	/**
	 * Constructor for IRLoad
	 * @param value of the variable to load, type ARRAY
	 */
	public IRLoad(VariableArray value)
	{
		this(value.getVar());
		this.type = value.getType();

		Variable indexVar = value.getAt();
		if (indexVar.getType() == Type.INTEGER)
			index = new IRConstant(indexVar.getVar());
		else
			index = new IRLoad(indexVar);

		this.addChild(index);
	}

	/**
	 * Returns the value of the field LoadedConstantValue
	 * @return	value of the field LoadedConstantValue
	 */
	public String getLoadedConstantValue()
	{
		return loadedConstantValue;
	}

	/**
	 * Returns the value of the field register
	 * @return	value of the field register
	 */
	public int getRegister()
	{
		return register;
	}

	/**
	 * Sets the value of the field register to the value of the parameter register
	 * @param register	new value for the field register
	 */
	public void setRegister(int register)
	{
		this.register = register;
	}

	/**
	 * Returns the value of the field index
	 * @return	value of the field index
	 */
	public IRNode getIndex()
	{
		return index;
	}

	/**
	 * Gets the instructions on code generation
	 * @return instructions list
	 */
	@Override
	public ArrayList<String> getInstructions()
	{
		IRMethod method = (IRMethod) findParent("Method");
		IRModule module = (IRModule) method.getParent();
		IRGlobal irGlobal = module.getGlobal(name);
		if (irGlobal != null)
			return getGlobalVariableInstructions(method);
		else
			return getLocalVariableInstructions(method);
	}

	/**
	 * get Local Variable Instructions
	 * @param method method parent of this object
	 * @return the list of instructions for local variables
	 */
	private ArrayList<String> getLocalVariableInstructions(IRMethod method)
	{
		ArrayList<String> inst = new ArrayList<>();
		int register = method.getArgumentRegister(name);
		if (register == -1)
		{
			IRAllocate var = method.getVarDeclaredUntilThis(name, this);

			// if var is const at this moment, we can put just its value and not load it
			// from register
			ArrayList<String> constantInstructions = getConstantCodeIfConstant(method);
			if (constantInstructions != null)
				return constantInstructions;

			register = var.getRegister();
		}
		if (register > -1)
		{
			if (type == Type.INTEGER)
			{
				// if var is const at this moment, we can put just its value and not load it
				// from register
				ArrayList<String> constantInstructions = getConstantCodeIfConstant(method);
				if (constantInstructions != null)
					return constantInstructions;
				inst.add(getInstructionToLoadIntFromRegisterToStack(register));
			} else
			{
				inst.add(getInstructionToLoadArrayFromRegisterToStack(register));
				if (arraySizeAccess)
					inst.add("arraylength");
				else if (index != null)
				{
					inst.addAll(index.getInstructions());
					inst.add("iaload");
				}
			}
		}

		return inst;
	}

	/**
	 * get Constant Code If Constant
	 * @param method method parent of this object
	 * @return the list of instructions for constant
	 */
	private ArrayList<String> getConstantCodeIfConstant(IRMethod method)
	{
		String varName = getVarNameForConstantName(name, index);
		IRConstant constValue = method.getConstValueByConstVarName(varName);
		if (constValue != null)
		{
			loadedConstantValue = constValue.getValue();
			return constValue.getInstructions(); // constant instructions
		}

		return null;
	}

	/**
	 * get global Variable Instructions
	 * @param method method parent of this object
	 * @return the list of instructions for globals variables
	 */
	private ArrayList<String> getGlobalVariableInstructions(IRMethod method)
	{
		ArrayList<String> inst = new ArrayList<>();
		inst.add(getGlobalVariableGetCodeByIRMethod(name, method));
		if (type == Type.INTEGER)
		{
			// if var is const at this moment, we can put just its value and not load it
			ArrayList<String> constantInstructions = getConstantCodeIfConstant(method);
			if (constantInstructions != null)
				return constantInstructions;
			else
				return inst;
		} else
		{
			if (arraySizeAccess)
				inst.add("arraylength");
			else if (index != null)
			{
				inst.addAll(index.getInstructions());
				inst.add("iaload");
			}
		}

		return inst;
	}

	/**
	 * Returns the value of the field type
	 * @return value of the field type
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * Sets the value of the field type to the value of the parameter type
	 * @param type	new value for the field type
	 */
	public void setType(Type type)
	{
		this.type = type;
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
	 * Returns the value of the field arraySizeAccess
	 * @return	value of the field arraySizeAccess
	 */
	public boolean isArraySizeAccess()
	{
		return arraySizeAccess;
	}

}
