package yal2jvm.hlir;

import java.util.ArrayList;

/**
 *	Class responsible for the intermediate representation for comparisons. Class that extend IRNode class.
 */
public class IRComparison extends IRNode
{
	private Comparator comp;
	private IRNode rhs;
	private IRNode lhs;
	private String label;

	/**
	 * Constructor for the class IRComparison using String operator
	 *
	 * @param operator operator used in comparison
	 * @param label label name to where jump in affirmative case
	 * @param invert boolean indication if is to invert condition
	 */
	public IRComparison(String operator, String label, boolean invert)
	{
		Comparator comp = getComparatorGivenOperator(operator);
		this.comp = invert ? Comparator.invert(comp) : comp;
		this.label = label;
		this.setNodeType("Comparison");
	}

	/**
	 * Receives a comparator as a String and returns it's Comparator enum equivalent
	 *
	 * @param operator
	 *            operator that will be checked for it's Comparator equivalent
	 * @return the Comparator enum value
	 */
	private Comparator getComparatorGivenOperator(String operator)
	{
		switch (operator)
		{
		case ">":
			return Comparator.GT;

		case "<":
			return Comparator.ST;

		case "<=":
			return Comparator.STE;

		case ">=":
			return Comparator.GTE;

		case "==":
			return Comparator.EQ;

		case "!=":
			return Comparator.NEQ;

		default:
			System.out.println("Unrecognized relational operator " + operator + ". Compile program will terminate.");
			System.exit(-1);
		}

		return null; // unreachable
	}

    /**
     * Gets the instructions for code generation
     * @return instructions list
     */
	@Override
	public ArrayList<String> getInstructions()
	{
		ArrayList<String> inst = new ArrayList<>();
		String branchInst;

		if (isConstantZero(rhs))
		{
			inst.addAll(lhs.getInstructions());

			branchInst = getZeroComparison();
		} else if (useArrayOperations())
		{
			inst.addAll(lhs.getInstructions());
			inst.addAll(rhs.getInstructions());

			branchInst = getArrayComparison();
		} else
		{
			inst.addAll(lhs.getInstructions());
			inst.addAll(rhs.getInstructions());

			branchInst = getIntegerComparison();
		}
		branchInst += " " + label;

		inst.add(branchInst);
		return inst;
	}

	/**
	 * Checks if node type is equal to constant and if it's value is equal to 0
	 *
	 * @param node
	 *            node that will be used to check it's content
	 * @return true if the node type is equal to constant and it's value is equal to
	 *         0. false if it fails at least one of these conditions.
	 */
	boolean isConstantZero(IRNode node)
	{
		if (node.getNodeType().equals("Constant"))
		{
			IRConstant constant = (IRConstant) node;
            return constant.getValue().equals("0");
		}
		return false;
	}

	/**
	 * Checks if it supposed to use array operations.
	 * 
	 * @return true if it is an array operation. False if not.
	 */
	private boolean useArrayOperations()
	{
		if (lhs.getNodeType().equals("Constant") || rhs.getNodeType().equals("Constant"))
			return false;

		if (rhs.getNodeType().equals("Load"))
		{
			IRLoad load = (IRLoad) rhs;
			if (load.getType() != Type.ARRAY || load.isArraySizeAccess())
				return false;

			if (lhs.getNodeType().equals("Load"))
			{
				load = (IRLoad) lhs;
                return load.getType() == Type.ARRAY && load.isArraySizeAccess() == false;
			}
		}

		return false;
	}

	/**
	 * get instruction to compare with 0, using the operator set in the constructor
	 * @return the instruction
	 */
	public String getZeroComparison()
	{
		String branchInst = "";
		switch (comp)
		{
		case EQ:
			branchInst = "ifeq";
			break;
		case GT:
			branchInst = "ifgt";
			break;
		case GTE:
			branchInst = "ifge";
			break;
		case NEQ:
			branchInst = "ifne";
			break;
		case ST:
			branchInst = "iflt";
			break;
		case STE:
			branchInst = "ifle";
			break;
		default:
			break;
		}
		return branchInst;
	}

	/**
     * get instruction to compare arrays, using the operator set in the constructor
     * @return the instruction
	 */
	public String getArrayComparison()
	{
		String branchInst = "";
		switch (comp)
		{
		case EQ:
			branchInst = "if_acmpeq";
			break;
		case NEQ:
			branchInst = "if_acmpne";
			break;
		default:
			break;
		}
		return branchInst;
	}

    /**
     * get instruction to compare integers, using the operator set in the constructor
     * @return the instruction
     */
	public String getIntegerComparison()
	{
		String branchInst = "";
		switch (comp)
		{
		case EQ:
			branchInst += "if_icmpeq";
			break;
		case GT:
			branchInst += "if_icmpgt";
			break;
		case GTE:
			branchInst += "if_icmpge";
			break;
		case NEQ:
			branchInst += "if_icmpne";
			break;
		case ST:
			branchInst += "if_icmplt";
			break;
		case STE:
			branchInst += "if_icmple";
			break;
		default:
			break;
		}
		return branchInst;
	}

	/**
	 * Returns the value of the field rhs
	 * 
	 * @return value of the field rhs
	 */
	public IRNode getRhs()
	{
		return rhs;
	}

	/**
	 * Sets the value of the field rhs to the value of the parameter rhs
	 * 
	 * @param rhs
	 */
	public void setRhs(IRNode rhs)
	{
		this.rhs = rhs;
		this.rhs.setParent(this);
	}

	/**
	 * Returns the value of the field lhs
	 * 
	 * @return value of the field lhs
	 */
	public IRNode getLhs()
	{
		return lhs;
	}

	/**
	 * Sets the value of the field lhs to the value of the parameter lhs
	 * 
	 * @param lhs
	 */
	public void setLhs(IRNode lhs)
	{
		this.lhs = lhs;
		this.lhs.setParent(this);
	}

	/**
	 * Returns the value of the field label
	 * 
	 * @return
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * Sets the value of the field label to the value of the parameter label
	 * 
	 * @param label
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}
}
