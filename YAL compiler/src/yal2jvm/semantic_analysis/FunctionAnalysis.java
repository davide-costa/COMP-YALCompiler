package yal2jvm.semantic_analysis;

import yal2jvm.ast.ASTFUNCTION;
import yal2jvm.ast.ASTSTATEMENTS;
import yal2jvm.ast.SimpleNode;
import yal2jvm.symbol_tables.FunctionSymbol;
import yal2jvm.symbol_tables.Symbol;
import yal2jvm.symbol_tables.VarSymbol;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Responsible for the Function semantic analysis
 */
public class FunctionAnalysis extends Analysis
{
	/**
	 * FunctionAnalysis constructor
	 * @param ast function tree node
	 * @param inheritedSymbols symbols from module or function
	 * @param functionNameToFunctionSymbolOfModule methods of the module, names to FunctionSymbol Object
	 */
	FunctionAnalysis(SimpleNode ast, HashMap<String, Symbol> inheritedSymbols,
			HashMap<String, Symbol> functionNameToFunctionSymbolOfModule)
	{
		super(ast, inheritedSymbols, functionNameToFunctionSymbolOfModule);
	}

	/**
	 * Parses the ast
	 */
	@Override
	protected void parse()
	{
		FunctionSymbol astFunction = (FunctionSymbol) functionNameToFunctionSymbol.get(((ASTFUNCTION) ast).id);

		addArgumentsToMySymbols(astFunction);
		addReturnValueToMySymbols(astFunction);

		int statementsChildNumber = astFunction.getStatementsChildNumber();
		ASTSTATEMENTS statementsNode = (ASTSTATEMENTS) ast.jjtGetChild(statementsChildNumber);
		parseStmtLst(statementsNode);

		// verify return value is defined if exists
		VarSymbol returnValue = astFunction.getReturnValue();
		if (returnValue != null)
		{
			if (!returnValue.isInitialized())
			{
				System.out.println("Line " + astFunction.getFunctionAST().getBeginLine() + ": Return variable "
						+ returnValue.getId() + " might not have been initialized. Function " + astFunction.getId()
						+ " must have return variable initialized.");
				ModuleAnalysis.hasErrors = true;
			}
		}
	}

	/**
	 * Adds the arguments to the function symbols
	 * @param functionSymbol the instance of the FunctionSymbol class from which the arguments must be added to symbols
	 */
	private void addArgumentsToMySymbols(FunctionSymbol functionSymbol)
	{
		ArrayList<VarSymbol> arguments = functionSymbol.getArguments();
		for (VarSymbol argument : arguments)
			mySymbols.put(argument.getId(), argument);
	}

	/**
	 * Adds the return value to the function symbols
	 * @param functionSymbol the instance of the FunctionSymbol class from which the arguments must be added to symbols
	 */
	private void addReturnValueToMySymbols(FunctionSymbol functionSymbol)
	{
		VarSymbol returnValue = functionSymbol.getReturnValue();
		if (returnValue != null)
			mySymbols.put(returnValue.getId(), returnValue);
	}

}
