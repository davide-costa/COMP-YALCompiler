package yal2jvm.symbol_tables;

import yal2jvm.ast.*;
import yal2jvm.semantic_analysis.ModuleAnalysis;

import java.util.ArrayList;

/**
 * FunctionSymbol class that extends the class Symbol
 */
public class FunctionSymbol extends Symbol
{

	private SimpleNode functionAST;
	private ArrayList<VarSymbol> arguments;
	private VarSymbol returnValue;
	private int statementsChildNumber = 0;

	/**
	 * Constructor for the class FunctionSymbol
	 * 
	 * @param functionAST
	 *            tree containing the subtrees of function
	 * @param id
	 *            object id
	 */
	public FunctionSymbol(SimpleNode functionAST, String id)
	{
		super(id);
		this.functionAST = functionAST;
		this.arguments = new ArrayList<>();
	}

	/**
	 * Returns the field functionAST
	 * 
	 * @return functionAST field value
	 */
	public SimpleNode getFunctionAST()
	{
		return functionAST;
	}

	/**
	 * Returns the field arguments
	 * 
	 * @return arguments field value
	 */
	public ArrayList<VarSymbol> getArguments()
	{
		return arguments;
	}

	/**
	 * Returns the field returnValue
	 * 
	 * @return returnValue field value
	 */
	public VarSymbol getReturnValue()
	{
		return returnValue;
	}

	/**
	 * Returns the statementsChildNumber field
	 * 
	 * @return statementsChildNumber field value
	 */
	public int getStatementsChildNumber()
	{
		return statementsChildNumber;
	}

	/**
	 * Calls functions that process the arguments and return value of a function
	 */
	public void parseFunctionHeader()
	{
		// indicates the index(child num) of the arguments. 0 if no return value, or 1
		// if has return value
		int argumentsIndex = 0;

		// get return value if existent
		SimpleNode returnValueNode = (SimpleNode) functionAST.jjtGetChild(0);
		if (returnValueNode instanceof ASTSTATEMENTS)
			return;

		if (!(returnValueNode instanceof ASTVARS))
		{
			argumentsIndex++;
			statementsChildNumber++;
			parseFunctionReturnValue(returnValueNode);
		}

		// get arguments if existent
		SimpleNode argumentsNode = (SimpleNode) functionAST.jjtGetChild(argumentsIndex);
		if (!(argumentsNode instanceof ASTVARS))
			return;

		statementsChildNumber++;
		parseArguments(argumentsNode);
	}

	/**
	 * Processes all of the arguments of a function, calling the function that
	 * process scalar elements and array elements.
	 * 
	 * @param argumentsNode
	 *            node containing all of the function's arguments
	 */
	private void parseArguments(SimpleNode argumentsNode)
	{
		for (int i = 0; i < argumentsNode.jjtGetNumChildren(); i++)
		{
			SimpleNode child = (SimpleNode) argumentsNode.jjtGetChild(i);
			if (child != null)
			{
				VarSymbol varSymbol;
				if (child instanceof ASTSCALARELEMENT)
					varSymbol = parseScalarElementArgument((ASTSCALARELEMENT) child);
				else
					varSymbol = parseArrayElementArgument((ASTARRAYELEMENT) child);

				if (varSymbol == null)
					continue;

				checkArgumentAlreadyExists(child, varSymbol);
				arguments.add(varSymbol);
			}
		}
	}

	/**
	 * Checks whether or not a function's argument already exists
	 * 
	 * @param child
	 *            argument node child
	 * @param varSymbol
	 *            argument being processed
	 */
	private void checkArgumentAlreadyExists(SimpleNode child, VarSymbol varSymbol)
	{
		for (VarSymbol argument : arguments)
		{
			if (argument.getId().equals(varSymbol.getId()))
			{
				System.out.println(
						"Line " + child.getBeginLine() + ": Argument " + varSymbol.getId() + " already declared.");
				ModuleAnalysis.hasErrors = true;
			}
		}
	}

	/**
	 * Parses an arrayElement argument
	 * @param child ASTARRAYELEMNT
	 * @return VarSymbol of the arrayElement
	 */
	private VarSymbol parseArrayElementArgument(ASTARRAYELEMENT child)
	{
		String astArrayElementId = child.id;
		String astArrayElementType = SymbolType.ARRAY.toString();
		if (returnValue != null && returnValue.getId().equals(astArrayElementId))
		{
			if (!returnValue.getType().equals(astArrayElementType))
			{
				System.out.println("Line " + child.getBeginLine() + ": Argument " + child.id
						+ " already declared as " + returnValue.getType() + ".");
				return null;
			} else
			{
				returnValue.setInitialized(true);
			}
		}

		return new VarSymbol(astArrayElementId, astArrayElementType, true);
	}

	/**
	 * Parses a scalarElement argument
	 * @param child ASTSCALARELEMENT
	 * @return VarSymbol with scalarElement
	 */
	private VarSymbol parseScalarElementArgument(ASTSCALARELEMENT child)
	{
		String astScalarElementId = child.id;
		String astScalarElementType = SymbolType.INTEGER.toString();
		if (returnValue != null && returnValue.getId().equals(astScalarElementId))
		{
			if (!returnValue.getType().equals(astScalarElementType))
			{
				System.out.println("Line " + child.getBeginLine() + ": Argument " + child.id
						+ " already declared as " + returnValue.getType() + ".");
				return null;
			} else
				returnValue.setInitialized(true);
		}

		return new VarSymbol(astScalarElementId, astScalarElementType, true);
	}

	/**
	 * Parses a function return value
	 * @param returnValueNode Node containing the return value
	 */
	private void parseFunctionReturnValue(SimpleNode returnValueNode)
	{
		if (returnValueNode instanceof ASTSCALARELEMENT)
		{
			ASTSCALARELEMENT astscalarelement = (ASTSCALARELEMENT) returnValueNode;
			String returnValueId = astscalarelement.id;
			returnValue = new VarSymbol(returnValueId, SymbolType.INTEGER.toString(), false);
		} else
		{
			ASTARRAYELEMENT astarrayelement = (ASTARRAYELEMENT) returnValueNode;
			String returnValueId = astarrayelement.id;
			returnValue = new VarSymbol(returnValueId, SymbolType.ARRAY.toString(), false);
		}
	}
}
