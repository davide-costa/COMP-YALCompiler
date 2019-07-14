package yal2jvm.hlir;

import yal2jvm.ast.ASTLHS;
import yal2jvm.ast.ASTRHS;

import java.util.ArrayList;

/**
 *	Class responsible for the intermediate representation for assign in a method of the module. Class that extend IRNode class.
 */
public class IRAssign
{
	String operator;
	boolean isSize = false;
	Variable lhs = null;
	ASTLHS astlhs;
	ASTRHS astrhs;
	ArrayList<Variable> operands = new ArrayList<>();

	/**
	 * Constructor for class IRAssign that takes the astLhs and astrhs values in the
	 * parameters to put them in their respective fields
	 * 
	 * @param astlhs
	 *            contains the lhs tree
	 * @param astrhs
	 *            contains the rhs tree
	 */
	IRAssign(ASTLHS astlhs, ASTRHS astrhs)
	{
		this.astlhs = astlhs;
		this.astrhs = astrhs;
	}
}
