package yal2jvm.hlir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import yal2jvm.Yal2jvm;
import yal2jvm.ast.*;
import yal2jvm.hlir.liveness_analysis.IntGraph;
import yal2jvm.hlir.liveness_analysis.LivenessAnalyzer;
import yal2jvm.hlir.register_allocation.RegisterAllocator;
import yal2jvm.utils.Utils;

/**
 * HLIR class
 */
public class HLIR
{
	private IRModule root;
	private HashMap<String, IntGraph> intGraphs;
	public static boolean optimize;
	public static HashMap<String, HashMap<String, Integer>> allocatedRegisterByMethodName;

	/**
	 * Creates a HLIR
	 * 
	 * @param ast ASTMODULE
	 */
	public HLIR(SimpleNode ast)
	{
		ASTMODULE astModule = (ASTMODULE) ast;
		createModuleHHIR(astModule);
	}

	/**
	 * Sets the optimize attribute to true
	 */
	public void setOptimize()
	{
		HLIR.optimize = true;
	}

	/**
	 * Performs the dataflow analysis
	 */
	public void dataflowAnalysis()
	{
		LivenessAnalyzer analyzer = new LivenessAnalyzer(this.root);
		analyzer.analyze();
		this.intGraphs = analyzer.getInterferenceGraphs();
	}

	/**
	 * Allocates registers
	 * 
	 * @param maxLocals
	 *            locals maximum
	 * @return true if the allocation could be done, false otherwise
	 */
	public boolean allocateRegisters(int maxLocals)
	{
		RegisterAllocator allocator = new RegisterAllocator(this.intGraphs);
		boolean allocateSuccessfully = allocator.allocate(maxLocals);

		allocatedRegisterByMethodName = allocator.getAllocatedRegisterByMethodName();

		if (allocateSuccessfully)
			assignNewRegisters(allocatedRegisterByMethodName);

		return allocateSuccessfully;
	}

	/**
	 * Prints the intermediate representation
	 */
	public void dumpIR()
	{
		System.out.println("\nHLIR of module " + this.root.getName() + ":\n");
		dumpIR(this.root, 0);
		System.out.println();
	}

	/**
	 * Recursive function used in dumpIR with no args
	 * 
	 * @param node
	 *            Node to be printed
	 * @param x
	 *            node level
	 */
	private void dumpIR(IRNode node, int x)
	{
		for (int i = 0; i < x; i++)
			System.out.print("  ");
		System.out.println(node.getNodeType());
		for (int i = 0; i < node.getChildren().size(); i++)
			dumpIR(node.getChildren().get(i), x + 1);
	}

	/**
	 * This method assigns the registers to the HLIR, according to the register
	 * allocation results, using auxiliar method assignNewRegistersMethod.
	 * 
	 * @param methods
	 *            hashMap that maps the method name to its own hashMap with it's
	 *            variables registers
	 */
	private void assignNewRegisters(HashMap<String, HashMap<String, Integer>> methods)
	{
		if (Yal2jvm.VERBOSE)
			System.out.println("\nRegisters assigned per method:");

		for (String key : methods.keySet())
			assignNewRegistersMethod(methods.get(key), key);
	}

	/**
	 * This method assigns the registers to the HLIR for a method, according to the
	 * register allocation results.
	 * 
	 * @param methodVars
	 *            hashMap with method's variables and corresponding registers
	 * @param methodName
	 *            the name of the method whose variable's registers will be set
	 */
	private void assignNewRegistersMethod(HashMap<String, Integer> methodVars, String methodName)
	{
		IRMethod method = null;

		if (Yal2jvm.VERBOSE)
			System.out.println("\nMethod " + methodName);

		for (IRNode child : this.root.getChildren())
		{
			if (child.getNodeType().equals("Method"))
			{
				if (((IRMethod) child).getName().equals(methodName))
				{
					method = (IRMethod) child;
					break;
				}
			}
		}

		TreeSet<Integer> uniqueRegs = new TreeSet<>();

		for (String key : methodVars.keySet())
		{
			if (Yal2jvm.VERBOSE)
				System.out.println("Var " + key + " -> " + methodVars.get(key));
			assert method != null;
			method.assignNewRegister(key, methodVars.get(key));
			uniqueRegs.add(methodVars.get(key));
		}
		assert method != null;
		method.setRegisterCount(uniqueRegs.size());
	}

	/**
	 * Gets the instructions from code generation
	 * 
	 * @return an arrayList with the instructions
	 */
	public ArrayList<String> selectInstructions()
	{
		ArrayList<String> inst = new ArrayList<>();
		inst.addAll(root.getInstructions());
		inst.addAll(getMethodClInit());

		return inst;
	}

	/**
	 * Gets clInit method instructions
	 * 
	 * @return an array list with the instructions
	 */
	private ArrayList<String> getMethodClInit()
	{
		ArrayList<String> inst = new ArrayList<>();

		ArrayList<String> globalStaticInstructions = getAllIRGlobalStaticInstructions();
		int maxStackSize = IRMethod.stackValueCount(globalStaticInstructions);
		if (globalStaticInstructions.size() != 0)
		{
			inst.add(".method public static <clinit>()V \n");
			inst.add(".limit stack " + maxStackSize + "\n");

			inst.addAll(globalStaticInstructions);

			inst.add("return \n");
			inst.add(".end method\n");
		}

		return inst;
	}

	/**
	 * This method goes over the HLIR and gets all instructions of IRGlobal objects,
	 * that need to be executed in the clinit method
	 * 
	 * @return list of the instructions it get
	 */
	private ArrayList<String> getAllIRGlobalStaticInstructions()
	{
		ArrayList<String> irGlobalsWithStaticInstructions = new ArrayList<>();

		for (IRNode child : root.children)
		{
			if (child.getNodeType().equals("Global"))
			{
				ArrayList<String> instructions = ((IRGlobal) child).getStaticArraysInstructions();
				if (instructions.size() != 0)
					irGlobalsWithStaticInstructions.addAll(instructions);
			}
		}

		return irGlobalsWithStaticInstructions;
	}

	/**
	 * Gets the module name
	 * 
	 * @return module name
	 */
	public String getModuleName()
	{
		return this.root.getName();
	}

	/**
	 * Creates the module intermediate representation
	 * 
	 * @param astModule
	 *            ASTMODULE
	 */
	private void createModuleHHIR(ASTMODULE astModule)
	{
		String moduleName = astModule.name;
		root = new IRModule(moduleName);

		int moduleNumberChilds = astModule.jjtGetNumChildren();
		for (int i = 0; i < moduleNumberChilds; i++)
		{
			Node child = astModule.jjtGetChild(i);
			if (child instanceof ASTDECLARATION)
				createDeclarationHHIR((ASTDECLARATION) child);
			else
				createFunctionHHIR((ASTFUNCTION) child);
		}
	}

	/**
	 * Creates the function intermediate representation
	 * 
	 * @param astFunction
	 *            ASTFUNCTION
	 */
	private void createFunctionHHIR(ASTFUNCTION astFunction)
	{
		String functionId = astFunction.id;
		Variable returnVar = new Variable(null, Type.VOID);
		Variable[] arguments = null;

		// indicates the index(child num) of the arguments. 0 if no return value, or 1
		// if has return value
		int argumentsIndex = 0;

		// get return value if existent
		SimpleNode currNode = (SimpleNode) astFunction.jjtGetChild(0);
		if (!(currNode instanceof ASTVARS) && !(currNode instanceof ASTSTATEMENTS)) // indicated that is the return
																					// variable
		{
			argumentsIndex++;
			if (currNode instanceof ASTSCALARELEMENT)
			{
				returnVar = new Variable(((ASTSCALARELEMENT) currNode).id, Type.INTEGER);
			} else if (currNode instanceof ASTARRAYELEMENT)
			{
				returnVar = new Variable(((ASTARRAYELEMENT) currNode).id, Type.ARRAY);
			} else
			{
				returnVar = new Variable(null, Type.VOID);
			}
		}

		// get arguments if existent
		currNode = (SimpleNode) astFunction.jjtGetChild(argumentsIndex);
		if (currNode instanceof ASTVARS)
		{
			int numArguments = currNode.jjtGetNumChildren();
			arguments = new Variable[numArguments];

			for (int i = 0; i < numArguments; i++)
			{
				SimpleNode child = (SimpleNode) currNode.jjtGetChild(i);
				if (child != null)
				{
					if (child instanceof ASTSCALARELEMENT)
					{
						arguments[i] = new Variable(((ASTSCALARELEMENT) child).id, Type.INTEGER);
					} else
					{
						arguments[i] = new Variable(((ASTARRAYELEMENT) child).id, Type.ARRAY);
					}
				}
			}
		}

		IRMethod function = new IRMethod(functionId, returnVar.getType(), arguments);
		root.addChild(function);

		// parse statements
		parseStatements(astFunction, returnVar, argumentsIndex, currNode, function);
	}

	/**
	 * Parses the function statements
	 * 
	 * @param astFunction
	 *            ASTFUNCTION
	 * @param returnVar
	 *            return variable
	 * @param argumentsIndex
	 *            arguments index
	 * @param currNode
	 *            current node
	 * @param function
	 *            IRMethod
	 */
	private void parseStatements(ASTFUNCTION astFunction, Variable returnVar, int argumentsIndex, SimpleNode currNode,
			IRMethod function)
	{
		if (!(currNode instanceof ASTSTATEMENTS))
			currNode = (SimpleNode) astFunction.jjtGetChild(++argumentsIndex);
		createStatementsHHIR((ASTSTATEMENTS) currNode, function);
		IRReturn irReturn = new IRReturn(returnVar);
		function.addChild(irReturn);
	}

	/**
	 * Creates the statements intermediate representation
	 * 
	 * @param aststatements
	 *            ASTSTATEMENTS
	 * @param irmethod
	 *            IRMethod
	 */
	private void createStatementsHHIR(ASTSTATEMENTS aststatements, IRMethod irmethod)
	{
		for (int i = 0; i < aststatements.jjtGetNumChildren(); i++)
		{
			SimpleNode child = (SimpleNode) aststatements.jjtGetChild(i);

			switch (child.toString())
			{
			case "ASSIGN":
				createAssignHHIR(child, irmethod);
				break;

			case "CALL":
				irmethod.addChild(getIRCall((ASTCALL) child, null));
				break;

			case "IF":
				createIfHHIR((ASTIF) child, irmethod);
				break;

			case "WHILE":
				createWhileHHIR((ASTWHILE) child, irmethod);
				break;

			default:
				System.out.println("Undefined statement. The compiler will terminate.");
				System.exit(-1);
			}
		}
	}

	/**
	 * Creates the If intermediate representation
	 * 
	 * @param astIf
	 *            ASTIF
	 * @param irmethod
	 *            IRMethod
	 */
	private void createIfHHIR(ASTIF astIf, IRMethod irmethod)
	{
		/*
		 * using template:
		 * 
		 * <do the test> <true_body> jump lab_end lab_false: <false_body> lab_end:
		 * 
		 */

		int labelNumber = root.getAndIncrementCurrLabelNumber();

		// test
		String labelFalse = "if_false" + labelNumber;
		String labelEnd = "if_end" + labelNumber;
		if (astIf.jjtGetNumChildren() > 2)
			createExprTestHHIR(astIf, irmethod, labelFalse, true);
		else
			createExprTestHHIR(astIf, irmethod, labelEnd, true);

		// true body
		ASTSTATEMENTS astIfStatements = (ASTSTATEMENTS) astIf.jjtGetChild(1);
		createStatementsHHIR(astIfStatements, irmethod);

		// if 2 childs, so else exists
		createElseHHIR(astIf, irmethod, labelFalse, labelEnd);

		// label true
		IRLabel irLabelEnd = new IRLabel(labelEnd);
		irmethod.addChild(irLabelEnd);
	}

	/**
	 * Creates the Else intermediate representation
	 * 
	 * @param astIf
	 *            ASTIF
	 * @param irmethod
	 *            IRMethod
	 * @param labelFalse
	 *            label for false statement
	 * @param labelEnd
	 *            label for end
	 */
	private void createElseHHIR(ASTIF astIf, IRMethod irmethod, String labelFalse, String labelEnd)
	{
		if (astIf.jjtGetNumChildren() > 2)
		{
			// jump end
			createJumpEndHHIR(irmethod, labelEnd);

			// label false
			IRLabel irLabelFalse = new IRLabel(labelFalse);
			irmethod.addChild(irLabelFalse);

			// false body
			ASTELSE astElse = (ASTELSE) astIf.jjtGetChild(2);
			ASTSTATEMENTS astElseStatements = (ASTSTATEMENTS) astElse.jjtGetChild(0);
			createStatementsHHIR(astElseStatements, irmethod);
		}
	}

	/**
	 * Gets the lhs IRNode
	 * 
	 * @param astLhs
	 *            ASTLHS
	 * @return lhs IRNode
	 */
	private IRNode getLhsIRNode(ASTLHS astLhs)
	{
		Node child = astLhs.jjtGetChild(0);
		if (child instanceof ASTARRAYACCESS)
		{
			ASTARRAYACCESS astArrayAccess = (ASTARRAYACCESS) child;
			Variable variable = getArrayAccessIRNode(astArrayAccess);
			return new IRLoad(new VariableArray(astArrayAccess.arrayID, variable));
		} else
		{
			ASTSCALARACCESS astScalarAccess = (ASTSCALARACCESS) child;
			Variable variable = new Variable(astScalarAccess.id, Type.INTEGER);
			return new IRLoad(variable);
		}
	}

	/**
	 * Gets the rhs intermediate representation node of Exprtest
	 * 
	 * @param astRhs
	 *            ASTRHS
	 * @return rhs IRNode of Exprtest
	 */
	private IRNode getRhsIRNodeOfExprtest(ASTRHS astRhs)
	{
		ArrayList<IRNode> termNodes = new ArrayList<>();
		int numTerms = astRhs.jjtGetNumChildren();
		for (int i = 0; i < numTerms; i++)
		{
			ASTTERM astTerm = (ASTTERM) astRhs.jjtGetChild(i);
			termNodes.add(getTermIRNode(astTerm));
		}

		// case just one term, return the respective IRNode, an IRoad
		if (numTerms == 1)
			return termNodes.get(0);

		// for multiples terms (2) return IRLoadArith
		IRLoadArith irLoadArith = new IRLoadArith(Operation.parseOperator(astRhs.operator));
		irLoadArith.setLhs(termNodes.get(0));
		irLoadArith.setRhs(termNodes.get(1));
		return irLoadArith;
	}

	/**
	 * Gets the term intermediate representation node
	 * 
	 * @param astTerm
	 *            ASTTERM
	 * @return term IRNode
	 */
	private IRNode getTermIRNode(ASTTERM astTerm)
	{
		String operator = astTerm.operator;

		Integer integer = astTerm.integer;
		if (integer != null)
		{
			if (operator.equals("-"))
				integer *= -1;
			return new IRConstant(integer.toString());
		}

		Node astTermChild = astTerm.jjtGetChild(0);
		if (astTermChild instanceof ASTCALL)
			return getIRCall((ASTCALL) astTermChild, null);
		else if (astTermChild instanceof ASTARRAYACCESS)
		{
			VariableArray variable = getArrayAccessIRNode((ASTARRAYACCESS) astTermChild);
			return new IRLoad(variable);
		} else
			return getScalarAccessIRNode((ASTSCALARACCESS) astTermChild);
	}

	/**
	 * Gets the scalar access intermediate representation node
	 * 
	 * @param astScalarAccess
	 *            ASTSCALARACCESS
	 * @return scalar access IRNode
	 */
	private IRNode getScalarAccessIRNode(ASTSCALARACCESS astScalarAccess)
	{
		String id = astScalarAccess.id;
		return new IRLoad(new Variable(id, Type.INTEGER));
	}

	/**
	 * Creates the while intermediate representation
	 * 
	 * @param astWhile
	 *            ASTWHILE
	 * @param irmethod
	 *            IRMethdod
	 */
	private void createWhileHHIR(ASTWHILE astWhile, IRMethod irmethod)
	{
		/*
		 * using template:
		 * 
		 * <test> lab_init: <body> <test> lab_end:
		 * 
		 */

		int labelNumber = root.getAndIncrementCurrLabelNumber();

		// test
		String labelEnd = "while_end" + labelNumber;
		createExprTestHHIR(astWhile, irmethod, labelEnd, true);

		// label init
		String labelInit = "while_init" + labelNumber;
		IRLabel irLabelInit = new IRLabel(labelInit);
		irmethod.addChild(irLabelInit);

		// body
		ASTSTATEMENTS astStatements = (ASTSTATEMENTS) astWhile.jjtGetChild(1);
		createStatementsHHIR(astStatements, irmethod);

		// test
		createExprTestHHIR(astWhile, irmethod, labelInit, false);

		// label end
		IRLabel irLabelEnd = new IRLabel(labelEnd);
		irmethod.addChild(irLabelEnd);
	}

	/**
	 * Creates the jump end intermediate representation
	 * 
	 * @param irmethod
	 *            IRMethod
	 * @param labelEnd
	 *            end label
	 */
	private void createJumpEndHHIR(IRMethod irmethod, String labelEnd)
	{
		IRJump irJump = new IRJump(labelEnd);
		irmethod.addChild(irJump);
	}

	/**
	 * Creates the exprtest intermediate representation
	 * 
	 * @param astNode
	 *            ASTNODE
	 * @param irmethod
	 *            IRMethod
	 * @param label
	 *            label
	 * @param invert
	 *            inverted conditions
	 */
	private void createExprTestHHIR(Node astNode, IRMethod irmethod, String label, boolean invert)
	{
		ASTEXPRTEST astExprtest = (ASTEXPRTEST) astNode.jjtGetChild(0);
		IRComparison irComparison = new IRComparison(astExprtest.operation, label, invert);

		ASTLHS astLhs = (ASTLHS) astExprtest.jjtGetChild(0);
		IRNode lhsIrNode = getLhsIRNode(astLhs);
		irComparison.setLhs(lhsIrNode);

		ASTRHS astRhs = (ASTRHS) astExprtest.jjtGetChild(1);
		IRNode rhsIrNode = getRhsIRNodeOfExprtest(astRhs);
		irComparison.setRhs(rhsIrNode);

		irmethod.addChild(irComparison);
	}

	/**
	 * Retrieves the name and may retrieve also type, value and operation for some
	 * variable
	 *
	 * @param child
	 *            simplenode
	 * @param irmethod
	 *            irmethod
	 */
	private void createAssignHHIR(SimpleNode child, IRMethod irmethod)
	{
		IRAssign irAssign = new IRAssign((ASTLHS) child.jjtGetChild(0), (ASTRHS) child.jjtGetChild(1));

		createAssignLhsHHIR(irAssign);

		irAssign.operator = irAssign.astrhs.operator; /* operator == null? IRAllocate : IRStoreArith */
		int numChildren = irAssign.astrhs.jjtGetNumChildren();
		for (int i = 0; i < numChildren; i++)
		{
			SimpleNode rhchild = (SimpleNode) irAssign.astrhs.jjtGetChild(i);
			createAssignRhsHHIR(irAssign, rhchild);
		}

		createAssignIR(irAssign, irmethod);
	}

	/**
	 * Creates the assign rhs intermediate representation
	 * 
	 * @param irAssign
	 *            IRAssign
	 * @param rhchild
	 *            rhchild
	 */
	private void createAssignRhsHHIR(IRAssign irAssign, SimpleNode rhchild)
	{
		switch (rhchild.toString())
		{
		case "TERM":
			ASTTERM term = (ASTTERM) rhchild;

			if (term.integer != null)
			{
				String str_value = term.operator + term.integer;
				irAssign.operands.add(new Variable(str_value, Type.INTEGER));
			} else
			{
				SimpleNode termChild = (SimpleNode) term.jjtGetChild(0);

				createAssignRhsVariableHHIR(irAssign, term, termChild);
			}
			break;
		case "ARRAYSIZE":
			ASTARRAYSIZE astarraysize = (ASTARRAYSIZE) rhchild;

			if (astarraysize.jjtGetNumChildren() == 0)
			{
				irAssign.operands.add(new Variable(astarraysize.integer.toString(), Type.INTEGER));
			} else
			{
				ASTSCALARACCESS astscalaraccess = (ASTSCALARACCESS) astarraysize.jjtGetChild(0);
				irAssign.operands.add(new Variable(astscalaraccess.id, Type.VARIABLE));
			}
			irAssign.isSize = true;
			break;
		}
	}

	/**
	 * Creates the assign rhs intermediate representation for variable access
	 * 
	 * @param irAssign
	 *            IRAssign
	 * @param term
	 *            ASTTERM
	 * @param termChild
	 *            termChild
	 */
	private void createAssignRhsVariableHHIR(IRAssign irAssign, ASTTERM term, SimpleNode termChild)
	{
		switch (termChild.toString())
		{
		case "CALL":

			ASTCALL astcall = (ASTCALL) termChild;
			IRCall irCall = getIRCall(astcall, irAssign.lhs.getVar());

			irAssign.operands.add(new VariableCall(null, Type.CALL, irCall));
			break;

		case "ARRAYACCESS":
			ASTARRAYACCESS astarrayaccess = ((ASTARRAYACCESS) termChild);
			ASTINDEX astindex = (ASTINDEX) termChild.jjtGetChild(0);
			String arrayaccess = term.operator + astarrayaccess.arrayID;

			if (astindex.indexID != null)
				irAssign.operands.add(new VariableArray(arrayaccess, new Variable(astindex.indexID, Type.VARIABLE)));
			else
				irAssign.operands.add(
						new VariableArray(arrayaccess, new Variable(astindex.indexValue.toString(), Type.INTEGER)));
			break;
		case "SCALARACCESS":
			String id = ((ASTSCALARACCESS) termChild).id;
			irAssign.operands.add(new Variable(id, Type.VARIABLE));
			break;
		}
	}

	/**
	 * Creates the assign lhs intermediate representation
	 * 
	 * @param irAssign
	 *            IRAssign
	 */
	private void createAssignLhsHHIR(IRAssign irAssign)
	{
		SimpleNode lhchild = (SimpleNode) irAssign.astlhs.jjtGetChild(0);
		switch (lhchild.toString())
		{
		case "ARRAYACCESS":
			irAssign.lhs = getArrayAccessIRNode((ASTARRAYACCESS) lhchild);
			break;

		case "SCALARACCESS":
			ASTSCALARACCESS astscalaraccess = (ASTSCALARACCESS) lhchild;

			irAssign.lhs = new Variable(astscalaraccess.id, Type.VARIABLE);
			break;
		}
	}

	/**
	 * Returns the array access IRNode
	 * 
	 * @param child
	 *            ASTARRAYACCESS
	 * @return array access IRNode
	 */
	private VariableArray getArrayAccessIRNode(ASTARRAYACCESS child)
	{
		ASTINDEX astindex = (ASTINDEX) child.jjtGetChild(0);

		if (astindex.indexID != null)
			return new VariableArray(child.arrayID, new Variable(astindex.indexID, Type.VARIABLE));
		else
			return new VariableArray(child.arrayID, new Variable(astindex.indexValue.toString(), Type.INTEGER));
	}

	/**
	 * Creates the assign intermediate representation for immediate values
	 * 
	 * @param irAssign
	 *            IRAssign
	 * @param irmethod
	 *            IRMethod
	 */
	private void createAssignImmediateIR(IRAssign irAssign, IRMethod irmethod)
	{
		IRStoreCall irStoreCall;
		Variable variable = irAssign.operands.get(0);

		if (variable.getType().equals(Type.CALL))
		{ // a = f1();
			if (irAssign.lhs.getType().equals(Type.VARIABLE))
			{
				irStoreCall = new IRStoreCall(irAssign.lhs.getVar());
				irStoreCall.addChild(((VariableCall) variable).getIrCall());
				irmethod.addChild(irStoreCall);
			} else
			{
				irStoreCall = new IRStoreCall((VariableArray) irAssign.lhs);
				irStoreCall.addChild(((VariableCall) variable).getIrCall());
				irmethod.addChild(irStoreCall);
			}
		} else
		{
			if (variable.getType().equals(Type.ARRAY))
			{
				if (irAssign.lhs.getType().equals(Type.ARRAY))
				{
					irmethod.addChild(new IRAllocate((VariableArray) irAssign.lhs, (VariableArray) variable));
				} else
				{
					irmethod.addChild(new IRAllocate(irAssign.lhs, (VariableArray) variable));
				}
			} else
			{
				if (!irAssign.isSize)
				{
					if (irAssign.lhs.getType().equals(Type.VARIABLE))
					{
						if (variable.getType().equals(Type.ARRAY))
						{ // a = b[5] // a = b[c] // a = b[c.size]
							irmethod.addChild(new IRAllocate(irAssign.lhs, (VariableArray) variable));
						} else
						{ // a = 3 // a = b // a = b.size
							irmethod.addChild(new IRAllocate(irAssign.lhs.getVar(), variable));
						}
					} else
					{ // a[X] = 3 // a[X] = b
						irmethod.addChild(new IRAllocate((VariableArray) irAssign.lhs, variable));
					}
				} else
				{ // a = [X]
					irmethod.addChild(new IRAllocate(irAssign.lhs.getVar(), variable, Type.ARRAYSIZE));
				}
			}
		}
	}

	/**
	 * Creates the assign intermediate representation for operations
	 * 
	 * @param irAssign
	 *            IRAssign
	 * @param irmethod
	 *            IRMethod
	 */
	private void createAssignOperationIR(IRAssign irAssign, IRMethod irmethod)
	{
		IRStoreArith irStoreArith;

		Variable var1 = irAssign.operands.get(0);
		Variable var2 = irAssign.operands.get(1);

		if (irAssign.lhs.getType().equals(Type.VARIABLE))
		{
			irStoreArith = new IRStoreArith(irAssign.lhs.getVar(), Operation.parseOperator(irAssign.operator));
		} else
		{
			irStoreArith = new IRStoreArith((VariableArray) irAssign.lhs, Operation.parseOperator(irAssign.operator));
		}

		setIRStoreArithLhs(irStoreArith, var1);
		setIRStoreArithRhs(irStoreArith, var2);

		boolean mayOptimize = var1.getType().equals(Type.INTEGER) && var2.getType().equals(Type.INTEGER);

		if (mayOptimize && optimize)
		{
			irmethod.addChild(new IRAllocate(irAssign.lhs.getVar(),
					new Variable(
							String.valueOf(Utils.getOperationValue(var1.getVar(), var2.getVar(), irAssign.operator)),
							Type.INTEGER)));
		} else
		{
			irmethod.addChild(irStoreArith);
		}
	}

	/**
	 * Sets lhs on irStoreArith
	 * 
	 * @param irStoreArith
	 *            IRStoreArith
	 * @param var1
	 *            variable on lhs
	 */
	private void setIRStoreArithLhs(IRStoreArith irStoreArith, Variable var1)
	{
		if (var1.getType().equals(Type.CALL))
		{ // a = f1() + X
			irStoreArith.setLhs(((VariableCall) var1).getIrCall());
		} else
		{
			if (var1.getType().equals(Type.INTEGER))
			{ // a = 3 + X
				irStoreArith.setLhs(new IRConstant(var1.getVar()));
			} else
			{
				if (var1.getType().equals(Type.VARIABLE))
				{ // a = b.size // a = b
					irStoreArith.setLhs(new IRLoad(var1));
				} else
				{ // a = b[c.size] // a = b[c]
					irStoreArith.setLhs(new IRLoad((VariableArray) var1));
				}
			}
		}
	}

	/**
	 * Sets rhs on irStoreArith
	 * 
	 * @param irStoreArith
	 *            IRStoreArith
	 * @param var2
	 *            variable on rhs
	 */
	private void setIRStoreArithRhs(IRStoreArith irStoreArith, Variable var2)
	{
		if (var2.getType().equals(Type.CALL))
		{ // a = f1() + X
			irStoreArith.setRhs(((VariableCall) var2).getIrCall());
		} else
		{
			if (var2.getType().equals(Type.INTEGER))
			{ // a = 3
				irStoreArith.setRhs(new IRConstant(var2.getVar()));
			} else
			{
				if (var2.getType().equals(Type.VARIABLE))
				{ // a = b.size // a = b
					irStoreArith.setRhs(new IRLoad(var2));
				} else
				{ // a = b[c.size] // a = b[c]
					irStoreArith.setRhs(new IRLoad((VariableArray) var2));
				}
			}
		}
	}

	/**
	 * Creates the assign intermediate representation
	 * 
	 * @param irAssign
	 *            IRAssign
	 * @param irmethod
	 *            IRMethod
	 */
	private void createAssignIR(IRAssign irAssign, IRMethod irmethod)
	{
		if (irAssign.operator.equals(""))
		{ // a = IMMEDIATE
			createAssignImmediateIR(irAssign, irmethod);
		} else
		{ // a = OPERATION
			createAssignOperationIR(irAssign, irmethod);
		}
	}

	/**
	 * Returns IRCall
	 * 
	 * @param astCall
	 *            ASTCALL
	 * @param lhsVarName
	 *            name of lhs variable
	 * @return IRCall
	 */
	private IRCall getIRCall(ASTCALL astCall, String lhsVarName)
	{
		String moduleId = astCall.module;
		String methodId = astCall.method;
		if (moduleId == null)
			moduleId = root.getName();
		ArrayList<Variable> arguments = new ArrayList<>();

		if (astCall.jjtGetNumChildren() > 0)
		{
			ASTARGUMENTS astarguments = (ASTARGUMENTS) astCall.jjtGetChild(0);
			arguments = getFunctionCallArgumentsIds(astarguments);
		}

		return new IRCall(methodId, moduleId, arguments, lhsVarName);
	}

	/**
	 * Returns function call arguments ids
	 * 
	 * @param astArguments
	 *            ASTARGUMENTS
	 * @return arguments variable arrayList
	 */
	private ArrayList<Variable> getFunctionCallArgumentsIds(ASTARGUMENTS astArguments)
	{
		ArrayList<Variable> arguments = new ArrayList<>();
		int numArguments = astArguments.jjtGetNumChildren();
		for (int i = 0; i < numArguments; i++)
		{
			ASTARGUMENT astArgument = (ASTARGUMENT) astArguments.jjtGetChild(i);
			if (astArgument.intArg != null)
			{
				Variable pair = new Variable(astArgument.intArg.toString(), Type.INTEGER);
				arguments.add(pair);
				continue;
			}
			if (astArgument.stringArg != null)
			{
				Variable pair = new Variable(astArgument.stringArg, Type.STRING);
				arguments.add(pair);
				continue;
			}
			if (astArgument.idArg != null)
			{
				Variable pair = new Variable(astArgument.idArg, Type.INTEGER);
				arguments.add(pair);
			}
		}

		return arguments;
	}

	/**
	 * Retrieves the name, type and value of the variable
	 *
	 * @param astdeclaration
	 *            declaration to analyse
	 */
	private void createDeclarationHHIR(ASTDECLARATION astdeclaration)
	{
		SimpleNode simpleNode = (SimpleNode) astdeclaration.jjtGetChild(0);
		Variable variable = null;
		Variable value = null;
		boolean arraySize = false;
		boolean initialized = true;

		switch (simpleNode.toString())
		{
		case "SCALARELEMENT":
			ASTSCALARELEMENT astscalarelement = (ASTSCALARELEMENT) simpleNode;
			variable = new Variable(astscalarelement.id, Type.VARIABLE);

			if (astdeclaration.jjtGetNumChildren() == 2)
			{
				value = createScalarElementDeclarationArraySizeHHIR(astdeclaration, variable);

				arraySize = true;
			} else // a = 4; a;
			{
				String str_value = astdeclaration.operator + astdeclaration.integer;

				if (astdeclaration.integer == null)
				{
					initialized = false;
				} else
				{
					value = new Variable(str_value, Type.INTEGER);
				}

				if (setGlobalVariableToArray(variable, value, initialized))
					return;

				variable.setType(Type.INTEGER);
			}
			break;
		case "ARRAYELEMENT":
			ASTARRAYELEMENT astarrayelement = (ASTARRAYELEMENT) simpleNode;
			variable = new Variable(astarrayelement.id, Type.ARRAY);

			if (astdeclaration.jjtGetNumChildren() == 2)
			{
				value = createArrayElementDeclarationArraySizeHHIR(astdeclaration);

				arraySize = true;
			} else
			{
				String str_value;

				if (astdeclaration.integer == null)
				{
					initialized = false;
				} else
				{
					str_value = astdeclaration.operator + astdeclaration.integer;
					value = new Variable(str_value, Type.INTEGER);
				}
			}
			break;
		}

		addChildToRoot(variable, value, arraySize, initialized);
	}

	/**
	 * Creates a scalarElement intermediate representation declaration for arraySize
	 * 
	 * @param astdeclaration
	 *            ASTDECLARATION
	 * @param variable
	 *            scalarElement variable
	 * @return variable value
	 */
	private Variable createScalarElementDeclarationArraySizeHHIR(ASTDECLARATION astdeclaration, Variable variable)
	{
		ASTARRAYSIZE astarraysize = (ASTARRAYSIZE) astdeclaration.jjtGetChild(1);
		Variable value = getArraySizeVariable(astdeclaration, astarraysize);

		variable.setType(Type.ARRAY);
		return value;
	}

	/**
	 * Creates an arrayElement intermediate representation declaration for arraySize
	 * 
	 * @param astdeclaration ASTDECLARATION
	 * @return arrayElement value
	 */
	private Variable createArrayElementDeclarationArraySizeHHIR(ASTDECLARATION astdeclaration)
	{
		Variable value;
		ASTARRAYSIZE astarraysize = (ASTARRAYSIZE) astdeclaration.jjtGetChild(1);
		if (astarraysize.jjtGetNumChildren() == 0)
		{
			value = new Variable(astarraysize.integer.toString(), Type.INTEGER);
		} else
		{
			ASTSCALARACCESS astscalaraccess = (ASTSCALARACCESS) astarraysize.jjtGetChild(0);
			value = new Variable(astscalaraccess.id, Type.VARIABLE);
		}
		return value;
	}

	/**
	 * Adds declaration child to root
	 * 
	 * @param variable
	 *            variable to add
	 * @param value
	 *            value to add
	 * @param arraySize
	 *            true if is array size, false otherwise
	 * @param initialized
	 *            true if is initialized, false otherwise
	 */
	private void addChildToRoot(Variable variable, Variable value, boolean arraySize, boolean initialized)
	{
		if (!initialized)
		{
			root.addChild(new IRGlobal(variable));
		} else
		{
			assert variable != null;
			switch (variable.getType())
			{
			case INTEGER:
				root.addChild(new IRGlobal(variable, value));
				break;
			case ARRAY:
				if (arraySize)
				{
					root.addChild(new IRGlobal(variable, value, Type.ARRAYSIZE));
				} else
				{
					root.addChild(new IRGlobal(variable, value));
				}
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Sets global variable to array
	 * 
	 * @param variable
	 *            variable to add
	 * @param value
	 *            value to add
	 * @param initialized
	 *            true if is initialized, false otherwise
	 * @return true if is now an array, false otherwise
	 */
	private boolean setGlobalVariableToArray(Variable variable, Variable value, boolean initialized)
	{
		IRGlobal irGlobal = root.getGlobal(variable.getVar());
		if (irGlobal != null && initialized)
		{
			variable.setType(Type.ARRAY);
			root.addChild(new IRGlobal(variable, value));
			return true;
		}
		return false;
	}

	/**
	 * Gets an arraySize variable
	 * 
	 * @param astdeclaration
	 *            the tree of the declaration
	 * @param astarraysize
	 *            the tree of the array size
	 * @return the created variable with the array size value
	 */
	private Variable getArraySizeVariable(ASTDECLARATION astdeclaration, ASTARRAYSIZE astarraysize)
	{
		Variable value;
		if (astarraysize.jjtGetNumChildren() == 0)
		{
			value = new Variable(astarraysize.integer.toString(), Type.INTEGER);
		} else
		{
			ASTSCALARACCESS astscalaraccess = (ASTSCALARACCESS) astarraysize.jjtGetChild(0);
			value = new Variable((astdeclaration.operator + astscalaraccess.id), Type.VARIABLE);
		}
		return value;
	}
}
