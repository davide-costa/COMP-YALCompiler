package yal2jvm.hlir;

import java.util.ArrayList;

/**
 * Class responsible for the intermediate representation for load of arith operation.
 */
public class IRLoadArith extends IRNode
{
	private IRArith irArith;

	/**
	 * Constructor for IRLoadArith
	 * @param op the operator used in the operation
	 */
	public IRLoadArith(Operation op)
	{
		this.setNodeType("LoadArith");
		irArith = new IRArith(op);
		this.addChild(irArith);
	}

	/**
	 * gets the right hand side of the operation
	 * @return the right hand side of the operation
	 */
	public IRNode getRhs()
	{
		return irArith.getRhs();
	}

	/**
	 * sets the right hand side of the operation
	 * @param rhs the right hand side of the operation
	 */
	public void setRhs(IRNode rhs)
	{
		this.irArith.setRhs(rhs);
	}

    /**
     * gets the left hand side of the operation
     * @return the left hand side of the operation
     */
	public IRNode getLhs()
	{
		return irArith.getLhs();
	}

    /**
     * sets the left hand side of the operation
     * @param lhs the left hand side of the operation
     */
	public void setLhs(IRNode lhs)
	{
		this.irArith.setLhs(lhs);
	}

	/**
	 * Gets the instructions for code generation
	 * @return instructions list
	 */
	@Override
	public ArrayList<String> getInstructions()
	{
		return irArith.getInstructions();
	}

}
