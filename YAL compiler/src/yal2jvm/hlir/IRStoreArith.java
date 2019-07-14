package yal2jvm.hlir;

import java.util.ArrayList;

/**
 *	Class responsible for the intermediate representation for store of the result of an arith operations. Class that extend IRNode class
 */
public class IRStoreArith extends IRStore
{
	private IRArith irArith;

	/**
	 * Default constructor used by other constructors of the class that has some
	 * basic and common actions.
	 * 
	 * @param op
	 *            the arith operation operator
	 */
	private IRStoreArith(Operation op)
	{
		this.setNodeType("StoreArith");
		irArith = new IRArith(op);
		this.addChild(irArith);
	}

	/**
	 * Constructor for IRStoreArith used when lhs (the variable being set) has type
	 * integer.
	 * 
	 * @param name
	 *            the lhs variable that will be set with the value of the arith
	 *            operation
	 * @param op
	 *            the arith operation operator
	 */
	// a = b + c
	IRStoreArith(String name, Operation op)
	{
		this(op);
		this.name = name;
	}

	/**
	 * Constructor for IRStoreArith used when lhs (the variable being set) has type
	 * array, and the arith result must be putted at a given index of the array.
	 * 
	 * @param name
	 *            the lhs variable that will be set with the value of the arith
	 *            operation
	 * @param op
	 *            the arith operation operator
	 */
	// a[i] = b + c;
	IRStoreArith(VariableArray name, Operation op)
	{
		this(op);
		this.name = name.getVar();
		this.arrayAccess = true;
		Variable at = name.getAt();
		if (at.getType().equals(Type.INTEGER))
			this.index = new IRConstant(at.getVar());
		else
			this.index = new IRLoad(at);
		this.addChild(index);
	}

	/**
	 * Returns the value of the field irArith Rhs field
	 * @return	value of the field irArith Rhs field
	 */
	public IRNode getRhs()
	{
		return irArith.getRhs();
	}

	/**
	 * Sets the value of the field irArith Rhs field to the value of the parameter rhs
	 * @param rhs	new value for the field irArith Rhs field
	 */
	public void setRhs(IRNode rhs)
	{
		this.irArith.setRhs(rhs);
	}

	/**
	 * Returns the value of the field irArith Lhs field
	 * @return alue of the field irArith Lhs field
	 */
	public IRNode getLhs()
	{
		return irArith.getLhs();
	}

	/**
	 * Sets the value of the field irArith Lhs field to the value of the parameter lhs
	 * @param lhs new value for the field irArith Lhs field
	 */
	public void setLhs(IRNode lhs)
	{
		this.irArith.setLhs(lhs);
	}

	/**
	 * Gets the instructions on code generation
	 * @return instructions list
	 */
	@Override
	public ArrayList<String> getInstructions()
	{
		ArrayList<String> inst = new ArrayList<>();
		boolean isIinc = checkIfIsIinc(inst);

		if (!isIinc)
		{
			ArrayList<String> storeInst = getInstForStoring(arrayAccess, index, irArith);
			inst.addAll(storeInst);
		}

		return inst;
	}

	/**
	 * Checks if it is an iinc
	 * @param inst instructions list
	 * @return true if is an iinc, false otherwise
	 */
	private boolean checkIfIsIinc(ArrayList<String> inst)
	{
		boolean isIinc = false;

		if (irArith.getOp().equals(Operation.ADD) || irArith.getOp().equals(Operation.SUB))
		{
			if (irArith.getLhs().getNodeType().equals("Load"))
			{
				IRLoad arithLhs = ((IRLoad) irArith.getLhs());

				if (arithLhs.getType().equals(Type.INTEGER) && getRhs().getNodeType().equals("Constant")
						&& arithLhs.getName().equals(getName()))
				{
					IRConstant irConstant = (IRConstant) getRhs();
					if (Integer.parseInt(irConstant.getValue()) > -32768
							&& Integer.parseInt(irConstant.getValue()) < 32768)
					{
						String instruction = getIincInstruction(irConstant);
						if (!instruction.equals(""))
						{
							inst.add(instruction);
							isIinc = true;
						}
					}
				}
			} else
			{
				if (irArith.getRhs().getNodeType().equals("Load"))
				{
					IRLoad arithRhs = ((IRLoad) irArith.getRhs());

					if (arithRhs.getType().equals(Type.INTEGER) && getLhs().getNodeType().equals("Constant")
							&& arithRhs.getName().equals(getName()))
					{
						IRConstant irConstant = (IRConstant) getLhs();
						if (Integer.parseInt(irConstant.getValue()) > -32768
								&& Integer.parseInt(irConstant.getValue()) < 32768)
						{
							String instruction = getIincInstruction(irConstant);
							if (!instruction.equals(""))
							{
								inst.add(instruction);
								isIinc = true;
							}
						}
					}
				}
			}
		}

		return isIinc;
	}

	/**
	 * gets the iinc instruction for the arith operation.
	 * Use only when already checked it can be done.
	 * @param irConstant constant with the increment value
	 * @return iinc instruction for the arith operation
	 */
	private String getIincInstruction(IRConstant irConstant)
	{
		String instruction = "iinc ";

		IRMethod method = (IRMethod) findParent("Method");
		int register = method.getArgumentRegister(name);
		if (register == -1)
			register = method.getVarRegister(name);
		if (register == -1)
			return "";

		addNewValueOfVariableNameToConstsHashMap(method, irConstant.getValue());

		instruction += register + " " + (irArith.getOp().equals(Operation.SUB) ? "-" : "") + irConstant.getValue();
		return instruction;
	}

	/**
	 * Increments the value of a const value if already exists in consts hashMap of the method parent of this IRStoreArith object
	 * @param method method parent of this IRStoreArith object
	 * @param increment the increment to the previous value of the constant value
	 */
	private void addNewValueOfVariableNameToConstsHashMap(IRMethod method, String increment)
	{
		IRConstant previousValue = method.getConstVarNameToConstValue().remove(name);
		if (previousValue == null)
			return;

		Integer newValueInteger = Integer.parseInt(previousValue.getValue()) + Integer.parseInt(increment);
		IRConstant newValue = new IRConstant(previousValue);
		newValue.setValue(newValueInteger.toString());
		method.getConstVarNameToConstValue().put(name, newValue);
	}

}
