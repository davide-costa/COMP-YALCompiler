package yal2jvm.semantic_analysis;

import yal2jvm.ast.ASTEXPRTEST;
import yal2jvm.ast.ASTSTATEMENTS;
import yal2jvm.ast.SimpleNode;
import yal2jvm.symbol_tables.Symbol;
import yal2jvm.utils.Utils;

import java.util.HashMap;

/**
 * Responsible for the While semantic analysis
 */
public class WhileAnalysis extends Analysis
{
	/**
	 * WhileAnalysis constructor
	 * @param ast while tree node
	 * @param inheritedSymbols symbols from module or function
	 * @param functionNameToFunctionSymbolOfModule methods of the module, names to FunctionSymbol Object
	 */
	WhileAnalysis(SimpleNode ast, HashMap<String, Symbol> inheritedSymbols,
				  HashMap<String, Symbol> functionNameToFunctionSymbolOfModule)
	{
		super(ast, inheritedSymbols, functionNameToFunctionSymbolOfModule);
	}

	/**
	 * Parses the ASTWHILE, taking into account the different scope of the while, and that variables declared inside are assumed to be uninitialized because while can not be executed
	 */
	@Override
	public void parse()
	{
		ASTEXPRTEST exprTest = ((ASTEXPRTEST) ast.jjtGetChild(0));
		parseExprTest(exprTest);

		// get inherited symbols States before while, in order to not change original
		// values.
		// Changes made inside while mus not be visible outside, because while can not
		// be executed
		inheritedSymbols = Utils.copyHashMap(inheritedSymbols);

		ASTSTATEMENTS stmtlst = ((ASTSTATEMENTS) ast.jjtGetChild(1));
		parseStmtLst(stmtlst);

		// symbols created inside while are added to symbol table, but as not
		// initialized, because while statements can not be executed
		mySymbols = setAllSymbolsAsNotInitialized(mySymbols);
	}

}
