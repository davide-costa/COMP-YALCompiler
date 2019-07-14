package yal2jvm.hlir;

import yal2jvm.Yal2jvm;
import yal2jvm.utils.Utils;

import java.util.ArrayList;

/**
 *	Class responsible for the intermediate representation for call to methods. Class that extend IRNode class.
 */
public class IRCall extends IRNode
{
	private String method;
	private String module;
	private String lhsVarName;
	private Type type;
	private ArrayList<Variable> arguments;

	/**
	 * Constructor for class IRCall
	 *
	 * @param method
	 * @param module
	 * @param arguments
	 * @param lhsVarName
	 */
	public IRCall(String method, String module, ArrayList<Variable> arguments, String lhsVarName)
	{
		this.method = method;
		this.module = module;
		this.arguments = arguments;
		this.lhsVarName = lhsVarName;
		this.nodeType = "Call";
	}

	/**
	 * Adds the arguments and call instructions to a new ArrayList variable and
	 * returns it
	 * 
	 * @return Arraylist containing the arguments and call instructions
	 */
	@Override
	public ArrayList<String> getInstructions()
	{
		ArrayList<String> inst = new ArrayList<>();

		inst.addAll(getArgumentsInstructions());

		String callInstructions = getCallInstruction();
		inst.add(callInstructions);

		if (getParent() instanceof IRMethod)
			if (Utils.isLastCharacterOfString("V", callInstructions) == false)
				inst.add("\npop");

		return inst;
	}

	/**
	 * Gets the instructions for code generation
	 * @return instructions list
	 */
	private ArrayList<String> getArgumentsInstructions()
	{
		ArrayList<String> inst = new ArrayList<>();

		if (this.method.equals("main"))
		{
			inst.add("aconst_null");
		} else
		{
			for (int i = 0; i < arguments.size(); i++)
			{
				Variable arg = arguments.get(i);

				Type type = arg.getType();

				type = getArgumentsType(arg, type);

				switch (type)
				{
				case STRING:
				{
					IRConstant stringConst = new IRConstant(arg.getVar());
					inst.addAll(stringConst.getInstructions());
					break;
				}
				case INTEGER:
				{
					IRNode var;
					if (arg.getVar().matches("-?\\d+"))
					{
						var = new IRConstant(arg.getVar());
					} else
					{
						var = new IRLoad(arg.getVar(), Type.INTEGER);
						this.addChild(var);
					}
					inst.addAll(var.getInstructions());
					break;
				}
				case ARRAY:
				{
					IRLoad irLoad = new IRLoad(arg.getVar(), Type.ARRAY);
					this.addChild(irLoad);
					inst.addAll(irLoad.getInstructions());
					break;
				}
				case ARRAYSIZE:
				{
					IRLoad irLoad = new IRLoad(arg.getVar(), Type.ARRAYSIZE);
					this.addChild(irLoad);
					inst.addAll(irLoad.getInstructions());
					break;
				}
				default:
					break;
				}
			}
		}

		return inst;
	}

	/**
	 * get arguments types
	 * @param arg argument
	 * @param initType initial type for the argument
	 * @return the type
	 */
	private Type getArgumentsType(Variable arg, Type initType)
	{
		IRMethod method = (IRMethod) findParent("Method");
		Type ret_type = initType;

		if (ret_type != Type.STRING)
		{
			ret_type = method.getArgumentType(arg.getVar());
			if (ret_type == null)
				ret_type = method.getVarType(arg.getVar());
			if (ret_type == null)
			{
				IRModule module = (IRModule) findParent("Module");
				IRGlobal global = module.getGlobal(arg.getVar());
				if (global != null)
					ret_type = global.getType();
			}
		}

		if (ret_type == null)
			return initType;
		else
			return ret_type;
	}

	/**
	 * Returns the value of the field type
	 * 
	 * @return value of field type
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * Gets the instructions of method call for code generation
	 * @return instructions list
	 */
	private String getCallInstruction()
	{
		String callInst = "invokestatic ";
		if (this.module != null)
			callInst += this.module + "/";
		callInst += this.method + "(";

		if (this.method.equals("main"))
		{
			callInst += "[Ljava/lang/String;";
		} else
		{
			for (int i = 0; i < arguments.size(); i++)
			{
				Type argumentType = arguments.get(i).getType();

				argumentType = getArgumentsType(arguments.get(i), argumentType);

				switch (argumentType)
				{
				case STRING:
				{
					callInst += "Ljava/lang/String;";
					break;
				}
				case INTEGER:
				{
					callInst += "I";
					break;
				}
				case ARRAY:
				{
					callInst += "[I";
					break;
				}
				case ARRAYSIZE:
				{
					callInst += "[I";
					break;
				}
				default:
					break;
				}
			}
		}
		callInst += ")";

		if (this.module == null || this.module.equals(Yal2jvm.moduleName))
		{
			IRModule irModule = (IRModule) findParent("Module");
			IRMethod irMethod = irModule.getChildMethod(method);
			Type returnType = irMethod.getReturnType();
			switch (returnType)
			{
			case INTEGER:
				callInst += "I";
				type = Type.INTEGER;
				break;
			case ARRAY:
				callInst += "[I";
				type = Type.ARRAY;
				break;
			case VOID:
				callInst += "V";
				type = Type.VOID;
				break;
			default:
				break;
			}
		} else // return undefined
		{
			// if call from statements, keep return undefined
			if (lhsVarName == null)
			{
				callInst += "V";
				type = Type.VOID;
				return callInst;
			}

			IRNode node = getVarIfExists(lhsVarName);
			if (node == null)
			{
				callInst += "I";
				type = Type.INTEGER;
				return callInst;
			}

			if (node instanceof IRAllocate)
			{
				IRAllocate allocate = (IRAllocate) node;
				if (allocate.getRegister() == -1)// if lhs not defined yet, we assume int
				{
					callInst += "I";
					type = Type.INTEGER;
				}

				if (allocate.getType().equals(Type.INTEGER)) // otherwise lhs defined, and type equals lhs var type
				{
					callInst += "I";
					type = Type.INTEGER;
				} else
				{
					callInst += "[I";
					type = Type.ARRAY;
				}
			} else if (node instanceof IRGlobal)
			{
				IRGlobal global = (IRGlobal) node;
				if (global.getType().equals(Type.INTEGER)) // otherwise lhs defined, and type equals lhs var type
				{
					callInst += "I";
					type = Type.INTEGER;
				} else
				{
					callInst += "[I";
					type = Type.ARRAY;
				}
			} else // argument
			{
				IRMethod method = (IRMethod) findParent("Method");
				if (method.getArgumentType(lhsVarName).equals(Type.INTEGER)) // otherwise lhs defined, and type equals
																				// lhs var type
				{
					callInst += "I";
					type = Type.INTEGER;
				} else
				{
					callInst += "[I";
					type = Type.ARRAY;
				}
			}
		}
		return callInst;
	}

	/**
	 * Returns the field arguments' value
	 * 
	 * @return the value of the field arguments
	 */
	public ArrayList<Variable> getArguments()
	{
		return arguments;
	}

	/**
	 * Sets the value of the field arguments to the parameter arguments
	 * 
	 * @param arguments
	 *            ArrayList of arguments that will be copied to the field arguments
	 */
	public void setArguments(ArrayList<Variable> arguments)
	{
		this.arguments = arguments;
	}
}
