package yal2jvm.semantic_analysis;

import yal2jvm.Yal2jvm;
import yal2jvm.ast.*;
import yal2jvm.hlir.Type;
import yal2jvm.symbol_tables.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Abstract class to represent the semantic analysis
 */
public abstract class Analysis
{

    HashMap<String, Symbol> mySymbols;
    HashMap<String, Symbol> inheritedSymbols;
    HashMap<String, Symbol> functionNameToFunctionSymbol;
    protected SimpleNode ast;

    /**
     * Constructor for the class analysis
     *
     * @param ast analysis tree
     * @param inheritedSymbols inherited symbols from previous scope
     * @param functionNameToFunctionSymbol methods of the module, names to FunctionSymbol Object
     */
    Analysis(SimpleNode ast, HashMap<String, Symbol> inheritedSymbols,
             HashMap<String, Symbol> functionNameToFunctionSymbol)
    {
        this.ast = ast;
        this.inheritedSymbols = inheritedSymbols;
        this.mySymbols = new HashMap<>();
        this.functionNameToFunctionSymbol = functionNameToFunctionSymbol;
    }

    /**
     * Abstract function that will be used by the analysis classes
     * that extend this class
     */
    protected abstract void parse();

    /**
     * Checks, separately, if the inheritedSymbols and the mySymbols hashMaps are set to null.
     * If they are both true, the elements of each checked hashMap are stored
     * inside another hashMap variable and the new variable is returned.
     * If one check is true and the other is not, the elements of the hashMap
     * that passed the validation will be stored inside the new hashMap variable.
     * If they are both false, the new hashMap variable will return an empty hashMap.
     *
     * @return  the hashmap containing all of the elements of inheritedSymbols
     *          plus all of the elements of mySymbols. Returns an empty hashMap when both
     *          are set to null.
     */
    HashMap<String, Symbol> getUnifiedSymbolTable()
    {
        HashMap<String, Symbol> unifiedSymbolTable = new HashMap<>();
        if (inheritedSymbols != null)
            unifiedSymbolTable.putAll(inheritedSymbols);
        if (mySymbols != null)
            unifiedSymbolTable.putAll(mySymbols);

        return unifiedSymbolTable;
    }

    /**
     * Checks if the value of the parameter symbolId is a key of the hashMap mySymbols.
     * If true, the value mapped to that key is stored in a new Symbol variable and is
     * returned.
     * If false, checks if the hashMap inheritedSymbols has a key with the parameter's
     * value.
     * If true, the value mapped to that key is stored in the new Symbol variable.
     * If all checks fail, the new variable is returned with a null value.
     *
     * @param symbolId  id of the symbol that will be used to check if it is contained
     *                  in any of the two hashMaps(mySymbols and inheritedSymbols)
     * @return          value mapped to the key symbolId in the mySymbols hashMap or
     *                  the inheritedSymbols hashmap if the first is set to null or
     *                  that key value doesn't exit. Return value is null when the key
     *                  doesn't exist in any of the two hashMaps
     */
    private Symbol hasAccessToSymbol(String symbolId)
    {
        Symbol symbol = null;
        if (mySymbols != null)
        {
            symbol = mySymbols.get(symbolId);
            if (symbol != null)
                return symbol;
        }

        if (inheritedSymbols != null)
            symbol = inheritedSymbols.get(symbolId);

        return symbol;
    }

    /**
     * Checks the type of the lhsTree first child.
     * If the child node is an array access the function parseArrayAccess is returned.
     * If the child node is a scalar access the function parseScalarAccess is returned.
     * If the child node isn't any of the two, no function is called a null value is returned.
     *
     * @param lhsTree   contains the entire lhs tree
     * @return          the functions that will process the two accesses. Array and Scalar.
     *                  Returns null if the child node isn't any of the two accesses.
     */
    private VarSymbol parseLhs(SimpleNode lhsTree)
    {
        Node child = lhsTree.jjtGetChild(0);
        switch (child.toString())
        {
            case "ARRAYACCESS":
                return parseArrayAccess((ASTARRAYACCESS) child);
            case "SCALARACCESS":
                return parseScalarAccess((ASTSCALARACCESS) child);
        }

        return null;
    }

    /**
     * Checks the type of the rhsTree first child.
     * If the child node is an "ARRAYSIZE" this node will be stored in a ASTARRAYSIZE variable.
     * It will then be processed by the parseArraySize function and it's return value stored
     * in a VarSymbol variable(retval). If the value of the variable retval is null, this function
     * will return null. If not, it will return a copy of itself with the type "ARRAYSIZE".
     * <p>
     * If the child node isn't of type "ARRAYSIZE", all of the rhs tree children will be processed.
     * This process will check if the children have the same type "term". Returning null when they
     * are of different type and when they are of type array. Returning the return value of the
     * parseTerm function of the last child processed.
     *
     * @param rhsTree   contains the entire rhs tree.
     * @return          null if the function fails the validations. ASTARRAYSIZE variable if the
     *                  first child is of type "ARRAYSIZE" and VarSymbol variable if not.
     */
    private VarSymbol parseRhs(SimpleNode rhsTree)
    {
        Node firstChild = rhsTree.jjtGetChild(0);
        if (firstChild.toString().equals("ARRAYSIZE"))
        {
            ASTARRAYSIZE astArraySize = (ASTARRAYSIZE) firstChild;
            VarSymbol retVal = parseArraySize(astArraySize);
            if (retVal == null)
                return null;

            retVal = retVal.getCopy();
            retVal.setType("ARRAYSIZE");
            return retVal;
        }

        VarSymbol symbol = null;
        String previousType = null;
        int numChildren = rhsTree.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++)
        {
            ASTTERM child = (ASTTERM) rhsTree.jjtGetChild(i);
            VarSymbol previousSymbol = symbol;
            symbol = parseTerm(child);
            if (symbol == null)
                return null;
            String symbolType = symbol.getType();
            if (previousType == null)
                previousType = symbolType;
            else if (!previousType.equals(symbolType))
            {
                System.out.println("Line " + child.getBeginLine() + ": Variables dont match! Variable "
                        + previousSymbol.getId() + " has type " + previousSymbol.getType()
                        + " and " + symbol.getId() + " has type " + symbol.getType() + ".");
                ModuleAnalysis.hasErrors = true;
                return null;
            }
            else if (previousType.equals(Type.ARRAY.toString()) && symbolType.equals(Type.ARRAY.toString()))
            {
                System.out.println("Line " + child.getBeginLine() + ": Cannot make operations between arrays.");
                ModuleAnalysis.hasErrors = true;
                return null;
            }

        }

        return symbol;
    }

    /**
     * Receives a variable of type "ASTARRAYSIZE" and if it is an integer, the function
     * returns a new object of type ImmediateSymbol with that variable as a parameter
     * for the constructor.
     * <p>
     * If the variable isn't an integer, then it is a scalar access. The first child of
     * the variable will be the parameter of the return function parseScalarAccess
     *
     * @param arraySizeTree contains the entire arraySize tree.
     * @return              object of type ImmediateSymbol, if arraySizeTree is an integer.
     *                      parseScalarAccess(child) if arraySizeTree isn't an integer.
     */
    private VarSymbol parseArraySize(ASTARRAYSIZE arraySizeTree)
    {
        if (arraySizeTree.integer != null)
        {
            return new ImmediateSymbol("[" + arraySizeTree.integer + "]");
        } else
        {
            ASTSCALARACCESS child = (ASTSCALARACCESS) arraySizeTree.jjtGetChild(0);
            return parseScalarAccess(child);
        }
    }

    /**
     * Receives a parameter of type "ASTTERM" and checks if it is an integer.
     * If it is, the functions returns a new object of type immediateSymbol with that
     * value as a parameter for the constructor.
     * If it isn't, the type of it's first child will be checked.
     * If it is of type "CALL", the function parseCall will process it and check for errors.
     * If it is of type "ARRAYACCESS" the function parseArrayAccess will be returned with that child.
     * If it is of type "SCALARACCESS" the function parseArrayAccess will be returned with that child.
     *
     * @param termTree  contains the entire term tree.
     * @return          object of type ImmediateSymbol. VarSymbol from the parseCall.
     *                  parseArrayAccess or parseScalarAccess function. Null
     */
    private VarSymbol parseTerm(ASTTERM termTree)
    {
        if (termTree.integer != null)
            return new ImmediateSymbol("[" + termTree.integer + "]");

        Node child = termTree.jjtGetChild(0);
        switch (child.toString())
        {
            case "CALL":

                ASTCALL astCall = (ASTCALL) child;
                VarSymbol callRet = parseCall(astCall);
                if(callRet == null)
                {
                    System.out.println("Line " + astCall.getBeginLine() + ": Method " + astCall.method + " is void. Expected return value.");
                    ModuleAnalysis.hasErrors = true;
                }
                return callRet;

            case "ARRAYACCESS":
                return parseArrayAccess((ASTARRAYACCESS) child);

            case "SCALARACCESS":
                return parseScalarAccess((ASTSCALARACCESS) child);

        }
        return null;
    }

    /**
     * Parses a given ASTCALL
     * @param callTree ASTCALL
     * @return FunctionSymbol of the function to which call is made, null otherwise
     */
    private VarSymbol parseCall(ASTCALL callTree)
    {
        String module = callTree.module;
        if (module != null && !module.equals(Yal2jvm.moduleName))
        {
            if (callTree.jjtGetNumChildren() > 0)
            {
                ASTARGUMENTS astarguments = (ASTARGUMENTS) callTree.jjtGetChild(0);
                if (parseArgumentList(astarguments) == null)
                    return null;
            }

            return new VarSymbol("", SymbolType.UNDEFINED.toString(), true);
        }

        String method = callTree.method;
        FunctionSymbol functionSymbol = (FunctionSymbol) functionNameToFunctionSymbol.get(method);
        if (functionSymbol == null)
        {
            System.out.println("Line " + callTree.getBeginLine() + ": Method " + method + " can't be found.");
            ModuleAnalysis.hasErrors = true;
            return null;
        }

        ArrayList<VarSymbol> functionArguments = functionSymbol.getArguments();
        VarSymbol returnSymbol = functionSymbol.getReturnValue();

        if (callTree.jjtGetNumChildren() == 0)
        {
            if (callTree.jjtGetNumChildren() != functionArguments.size())
            {
                System.out.println("Line " + callTree.getBeginLine() + ": Method " + method + " arguments number(0)"
                        + "does not match expected number(" + functionArguments.size() + ") of arguments");
                ModuleAnalysis.hasErrors = true;
                return null;
            }
        } else
        {
            ASTARGUMENTS astarguments = (ASTARGUMENTS) callTree.jjtGetChild(0);
            ArrayList<String> argumentsTypes = parseArgumentList(astarguments);
            if (argumentsTypes == null)
                return null;

            if (functionArguments.size() != argumentsTypes.size())
            {
                System.out.println("Line " + astarguments.getBeginLine() + ": Method " + method + " arguments number("
                        + argumentsTypes.size() + ") does not match expected number(" + functionArguments.size() + ") of arguments");
                ModuleAnalysis.hasErrors = true;
                return null;
            }

            returnSymbol = parseCallArguments(method, functionArguments, returnSymbol, astarguments, argumentsTypes);
        }

        if (returnSymbol == null)
            return null;

        returnSymbol = returnSymbol.getCopy();
        returnSymbol.setInitialized(true);

        return returnSymbol;
    }

    /**
     * Parses the ASTCALL ArgumentList
     * @param method method
     * @param functionArguments list of arguments
     * @param returnSymbol return varSymbol
     * @param astarguments ASTARGUMENTS
     * @param argumentsTypes list of argumentTypes
     * @return returnSymbol
     */
    private VarSymbol parseCallArguments(String method, ArrayList<VarSymbol> functionArguments, VarSymbol returnSymbol, ASTARGUMENTS astarguments, ArrayList<String> argumentsTypes) {
        for (int i = 0; i < functionArguments.size(); i++)
        {
            String argumentType = argumentsTypes.get(i);
            String expectedArgumentType = functionArguments.get(i).getType();
            if (!argumentType.equals(expectedArgumentType))
            {
                System.out.println("Line " + astarguments.getBeginLine() + ": Type " + argumentType
                        + " of argument " + i + 1 + " of method " + method
                        + " call does not match expected type " + expectedArgumentType + ".");
                ModuleAnalysis.hasErrors = true;
                returnSymbol = null;
            }
        }
        return returnSymbol;
    }

    /**
     * Parses the argument list
     * @param astarguments ASTARGUMENTS
     * @return list of arguments types
     */
    private ArrayList<String> parseArgumentList(ASTARGUMENTS astarguments)
    {
        Integer childrenLength = astarguments.jjtGetNumChildren();
        ArrayList<String> argumentsTypes = new ArrayList<>();
        boolean haveFailed = false;
        for (int i = 0; i < childrenLength; i++)
        {
            ASTARGUMENT astargument = ((ASTARGUMENT) astarguments.jjtGetChild(i));
            String idArg = astargument.idArg;
            Integer intArg = astargument.intArg;
            String stringArg = astargument.stringArg;

            if (idArg == null && intArg == null && stringArg == null)
            {
                System.out.println("Line " + astargument.getBeginLine() + ": Argument " + i + " is neither a variable,"
                        + "a string or an integer.");
                ModuleAnalysis.hasErrors = true;
                return null;
            }

            if (idArg != null)
            {
                VarSymbol varSymbol = (VarSymbol) checkSymbolExistsAndIsInitialized(astargument, idArg);
                if (varSymbol == null)
                {
                    haveFailed = true;
                    continue;
                }
                argumentsTypes.add(varSymbol.getType());
            } else if (intArg != null)
                argumentsTypes.add(SymbolType.INTEGER.toString());
            else
                argumentsTypes.add("STRING");
        }

        if (haveFailed)
            return null;

        return argumentsTypes;
    }

    /**
     * Parses an array access
     * @param arrayAccessTree ASTARRAYACCESS
     * @return array varSymbol
     */
    private VarSymbol parseArrayAccess(ASTARRAYACCESS arrayAccessTree)
    {
        String arrayId = arrayAccessTree.arrayID;

        VarSymbol arraySymbol = (VarSymbol) checkSymbolExistsAndIsInitialized(arrayAccessTree, arrayId);
        if (arraySymbol == null)
            return null;

        if (!arraySymbol.getType().equals(SymbolType.ARRAY.toString()))
        {
            System.out.println("Line " + arrayAccessTree.getBeginLine() + ": Access to index of variable " + arrayId
                    + " that is not an array.");
            ModuleAnalysis.hasErrors = true;
            return null;
        }

        ASTINDEX astindex = (ASTINDEX) arrayAccessTree.jjtGetChild(0);
        String indexValue = null;

        if (astindex.indexValue != null)
            indexValue = astindex.indexValue.toString();

        String indexSymbolId = astindex.indexID;
        if (indexSymbolId != null)
        {
            VarSymbol indexSymbol = (VarSymbol) checkSymbolExistsAndIsInitialized(astindex, indexSymbolId);
            if (indexSymbol == null)
                return null;
            indexValue = indexSymbol.getId();
        }

        arraySymbol = arraySymbol.getCopy();
        arraySymbol.setType(SymbolType.INTEGER.toString());
        arraySymbol.setId(arraySymbol.getId() + "[" + indexValue + "]");

        return arraySymbol;
    }

    /**
     * Checks if a given symbol exists and is initialized
     * @param ast tree node
     * @param symbolId symbol name
     * @return index of the symbol
     */
    private Symbol checkSymbolExistsAndIsInitialized(SimpleNode ast, String symbolId)
    {
        VarSymbol indexSymbol = (VarSymbol) hasAccessToSymbol(symbolId);
        if (indexSymbol == null)
        {
            System.out.println("Line " + ast.getBeginLine() + ": Variable " + symbolId + " might not have been declared.");
            ModuleAnalysis.hasErrors = true;
            return null;
        }

        if (!indexSymbol.isInitialized())
        {
            System.out.println("Line " + ast.getBeginLine() + ": Variable " + symbolId + " might not have been initialized.");
            ModuleAnalysis.hasErrors = true;
            return null;
        }

        return indexSymbol;
    }

    /**
     * Parses a scalar access
     * @param scalarAccessTree ASTSCALARACCESS
     * @return scalarAccess varSymbol
     */
    private VarSymbol parseScalarAccess(ASTSCALARACCESS scalarAccessTree)
    {
        String id = scalarAccessTree.id;
        boolean sizeAccess = false;
        if (id.contains("."))
        {
            int dotIdx = id.indexOf(".");
            if (id.substring(dotIdx + 1).equals("size"))
                sizeAccess = true;
            id = id.substring(0, dotIdx);
        }

        if (sizeAccess)
        {
            VarSymbol varSymbol = (VarSymbol) hasAccessToSymbol(id);
            if(varSymbol == null)
            {
                System.out.println("Line " + scalarAccessTree.getBeginLine() + ": Variable " + id + " might not have been declared.");
                ModuleAnalysis.hasErrors = true;
                return null;
            }

            if (varSymbol.getType().equals("INTEGER"))
            {
                System.out.println("Line " + scalarAccessTree.getBeginLine() + ": Access to size of variable " + id
                        + " that is not an array.");
                ModuleAnalysis.hasErrors = true;
                return null;
            }

            varSymbol = varSymbol.getCopy();
            varSymbol.setType(SymbolType.INTEGER.toString());
            return varSymbol;
        }

        return (VarSymbol) checkSymbolExistsAndIsInitialized(scalarAccessTree, id);
    }

    /**
     * Parses a declaration
     * @param declarationTree ASTDECLARATION
     */
    void parseDeclaration(ASTDECLARATION declarationTree)
    {
        Node child = declarationTree.jjtGetChild(0);
        if (child instanceof ASTSCALARELEMENT)
        {
            ASTSCALARELEMENT astscalarelement = (ASTSCALARELEMENT) child;
            parseDeclarationAstScalarElement(declarationTree, astscalarelement);

        } else if (child instanceof ASTARRAYELEMENT)
        {
            ASTARRAYELEMENT astarrayelement = (ASTARRAYELEMENT) child;
            parseDeclarationAstArrayElement(declarationTree, astarrayelement);
        }

    }

    /**
     * Parses a declaration AST scalarElement
     * @param declarationTree ASTDECLARATION
     * @param astscalarelement ASTSCALARELEMENT
     */
    private void parseDeclarationAstScalarElement(ASTDECLARATION declarationTree, ASTSCALARELEMENT astscalarelement)
    {
        VarSymbol symbol = (VarSymbol) hasAccessToSymbol(astscalarelement.id);
        if (symbol != null) {
            parseDeclarationSymbol(declarationTree, symbol);
            return;
        }

        VarSymbol varSymbol = createSymbolForDeclarationAstScalarElement(declarationTree, astscalarelement);
        if (varSymbol == null)
            return;

        mySymbols.put(varSymbol.getId(), varSymbol);
    }

    /**
     * Creates a symbol for a declaration AST scalarElement
     * @param declarationTree ASTDECLARATION
     * @param astscalarelement ASTSCALARELEMENT
     * @return scalarElement varSymbol
     */
    private VarSymbol createSymbolForDeclarationAstScalarElement(ASTDECLARATION declarationTree, ASTSCALARELEMENT astscalarelement)
    {
        boolean initialized = false;
        if (declarationTree.integer != null) //if is from type a=CONST;
            initialized = true;

        VarSymbol varSymbol = new VarSymbol(astscalarelement.id, SymbolType.INTEGER.toString(), initialized);

        if (declarationTree.jjtGetNumChildren() > 1) //if is from type a=[CONST];
        {
            Node child = declarationTree.jjtGetChild(1);
            ASTARRAYSIZE astarraysize = (ASTARRAYSIZE) child;
            if (astarraysize.integer == null)
            {
                ASTSCALARACCESS astScalarAccess = (ASTSCALARACCESS) astarraysize.jjtGetChild(0);
                VarSymbol scalarAccessSymbol = parseScalarAccess(astScalarAccess);

                if (scalarAccessSymbol == null)
                    return null;
            }

            varSymbol.setType(SymbolType.ARRAY.toString());
            varSymbol.setInitialized(true);
        }

        return varSymbol;
    }

    /**
     * Parses a declaration AST arrayElement
     * @param declarationTree ASTDECLARATION
     * @param astarrayelement ASTARRAYELEMENT
     */
    private void parseDeclarationAstArrayElement(ASTDECLARATION declarationTree, ASTARRAYELEMENT astarrayelement)
    {
        VarSymbol symbol = (VarSymbol) hasAccessToSymbol(astarrayelement.id);
        if (symbol != null) {
            parseDeclarationSymbol(declarationTree, symbol);
            return;
        }

        VarSymbol varSymbol = createSymbolForDeclarationAstArrayElement(declarationTree, astarrayelement);
        if (varSymbol == null)
            return;

        mySymbols.put(varSymbol.getId(), varSymbol);
    }

    /**
     * Creates a symbol for declaration AST arrayElement
     * @param declarationTree ASTDECLARATION
     * @param astarrayelement ASTARRAYELEMENT
     * @return arrayElement varSymbol
     */
    private VarSymbol createSymbolForDeclarationAstArrayElement(ASTDECLARATION declarationTree, ASTARRAYELEMENT astarrayelement)
    {
        boolean initialized;
        if (declarationTree.jjtGetNumChildren() > 1) //if is from type a[]=[CONST];
        {
            Node child = declarationTree.jjtGetChild(1);
            ASTARRAYSIZE astarraysize = (ASTARRAYSIZE) child;
            if (astarraysize.integer == null)
            {
                ASTSCALARACCESS astScalarAccess = (ASTSCALARACCESS) astarraysize.jjtGetChild(0);
                VarSymbol scalarAccessSymbol = parseScalarAccess(astScalarAccess);
                if (scalarAccessSymbol == null)
                    return null;
            }
            initialized = true;
        }
        else
        {
            //if from type a[] = CONST; and variable array has no size set (its not declared even)
            if (declarationTree.integer != null)
            {
                System.out.println("Line " + declarationTree.getBeginLine() + ": Variable "
                        + astarrayelement.id + " has the size not defined." + " Error assigning "
                        + declarationTree.integer + " to all elements of " + astarrayelement.id + ".");
                ModuleAnalysis.hasErrors = true;
                return null;
            }

            //if from type a[]; variable not initialized and size = -1
            initialized = false;
        }

        return new VarSymbol(astarrayelement.id, SymbolType.ARRAY.toString(), initialized);
    }

    /**
     * Parses a declaration symbol
     * @param declarationTree ASTDECLARATION
     * @param symbol declaration symbol
     */
    private void parseDeclarationSymbol(ASTDECLARATION declarationTree, VarSymbol symbol)
    {
        //if it has already been declared and its not just a initialization
        if (declarationTree.integer == null)
        {
            System.out.println("Line " + declarationTree.getBeginLine() + ": Variable " + symbol.getId()
                    + " already declared.");
            ModuleAnalysis.hasErrors = true;
            return;
        }

        if (symbol.getType().equals(Type.INTEGER.toString()) && symbol.isInitialized())
        {
            System.out.println("Line " + declarationTree.getBeginLine() + ": Variable "
                    + symbol.getId() + " was already initialized." + " Error assigning "
                    + declarationTree.integer + " to the variable " + symbol.getId() + ".");
            ModuleAnalysis.hasErrors = true;
            return;
        }

        symbol.setInitialized(true);

        if (symbol.getType().equals(Type.ARRAY.toString()) && !symbol.isInitialized())
        {
            System.out.println("Line " + declarationTree.getBeginLine() + ": Variable "
                    + symbol.getId() + " has the size not defined." + " Error assigning "
                    + declarationTree.integer + " to all elements of " + symbol.getId() + ".");
            ModuleAnalysis.hasErrors = true;
        }

    }

    /**
     * Parses an assign
     * @param assignTree ASTASSIGN
     */
    private void parseAssign(ASTASSIGN assignTree)
    {
        VarSymbol rhsSymbol = null;
        SimpleNode rhsTree = (SimpleNode) assignTree.jjtGetChild(1);
        if (rhsTree != null)
            rhsSymbol = parseRhs(rhsTree);

        SimpleNode lhsTree = (SimpleNode) assignTree.jjtGetChild(0);
        VarSymbol lhsSymbol = getLhsVariable(lhsTree);
        if (lhsSymbol == null)
            return;

        if (initalizeLhsWithNullRhsAssign(rhsSymbol, lhsSymbol)) return;

        if (lhsSymbol.getId().contains(".size"))
        {
            assert rhsTree != null;
            System.out.println("Line " + rhsTree.getBeginLine() + ": Impossible to set a variable size.");
            ModuleAnalysis.hasErrors = true;
            return;
        }

        assert rhsSymbol != null;
        if (parseArraySizeAssign(rhsSymbol, lhsSymbol)) return;

        if (lhsSymbol.getType().equals(SymbolType.UNDEFINED.toString()))
        {
            if (rhsSymbol.getType().equals(SymbolType.UNDEFINED.toString()))
                lhsSymbol.setType(SymbolType.INTEGER.toString());
            else
                lhsSymbol.setType(rhsSymbol.getType());
        }

        String lhsSymbolType = lhsSymbol.getType();
        String rhsSymbolType = rhsSymbol.getType();

        if (lhsSymbolType.equals(rhsSymbolType) && !lhsSymbol.isArrayAccess() && !(rhsSymbol instanceof ImmediateSymbol)) //if both lhs and rhs have same type
        {
            lhsSymbol.setInitialized(rhsSymbol.isInitialized());
            addToSymbolTable(lhsSymbol);
            return;
        }

        if (errorArrayLhsIntegerRhsSizeNotDefined(lhsTree, lhsSymbol, lhsSymbolType, rhsSymbolType)) return;

        //for A=[N] in which N is an integer. Used when assigning size to an array
        if (lhsSymbolType.equals(SymbolType.ARRAY.toString()) && rhsSymbolType.equals("ARRAYSIZE"))
        {
            lhsSymbol.setInitialized(true);
            addToSymbolTable(lhsSymbol);
            return;
        }

        if (checkMatchingTypesAssign(lhsTree, lhsSymbol, lhsSymbolType, rhsSymbolType)) return;

        lhsSymbol.setInitialized(true);

        addToSymbolTable(lhsSymbol);
    }

    /**
     * For the case in which the array as not the size defined yet
     * @param lhsTree tree of lhs
     * @param lhsSymbol symbol on lhs
     * @param lhsSymbolType lhs symbol type
     * @param rhsSymbolType rhs symbol type
     * @return true if there was an error, false otherwise
     */
    private boolean errorArrayLhsIntegerRhsSizeNotDefined(SimpleNode lhsTree, VarSymbol lhsSymbol, String lhsSymbolType, String rhsSymbolType) {
        if (lhsSymbolType.equals(SymbolType.ARRAY.toString()) && rhsSymbolType.equals("INTEGER") && !lhsSymbol.isInitialized())
        {
            System.out.println("Line " + lhsTree.getBeginLine() + ": Variable " + lhsSymbol.getId()
                    + " has the size not defined." + " Error assigning right hand side to all elements of " + lhsSymbol.getId() + ".");
            ModuleAnalysis.hasErrors = true;
            return true;
        }
        return false;
    }

    /**
     * If rhs has an error, but lhs is correct, we assume that lhs is initialized
     * @param rhsSymbol symbol on rhs
     * @param lhsSymbol symbol on lhs
     * @return true if lhs was initialized, false otherwise
     */
    private boolean initalizeLhsWithNullRhsAssign(VarSymbol rhsSymbol, VarSymbol lhsSymbol) {
        if (rhsSymbol == null)
        {
            lhsSymbol.setInitialized(true);
            addToSymbolTable(lhsSymbol);
            return true;
        }
        return false;
    }

    /**
     * Checks if lhs and rhs have matching types
     * @param lhsTree tree of lhs
     * @param lhsSymbol symbol on lhs
     * @param lhsSymbolType lhs symbol type
     * @param rhsSymbolType rhs symbol type
     * @return true if they don't match, false otherwise
     */
    private boolean checkMatchingTypesAssign(SimpleNode lhsTree, VarSymbol lhsSymbol, String lhsSymbolType, String rhsSymbolType) {
        if (!(lhsSymbolType.equals(SymbolType.ARRAY.toString()) && rhsSymbolType.equals(SymbolType.INTEGER.toString()))) //for A=5; in which A is an array and all its elements are set to 5
            if (!rhsSymbolType.equals(SymbolType.UNDEFINED.toString())) //for A=m.f(); in which m.f() function is from another module that we not know the return value, so it can be INTEGER or ARRAY
                if (!lhsSymbolType.equals(rhsSymbolType) || (rhsSymbolType.equals(SymbolType.ARRAY.name()) && lhsSymbol.isArrayAccess())) //checks both have types that match
                {
                    if(lhsSymbol.isArrayAccess())
                    {
                        System.out.println("Line " + lhsTree.getBeginLine() + ": Variable " + lhsSymbol.getId()
                                + " of type ARRAY of INTEGERS, accessed at an index, so type INTEGER. " +
                                "Cannot redeclare it as " + rhsSymbolType + ".");
                    }
                    else
                    {
                        System.out.println("Line " + lhsTree.getBeginLine() + ": Variable " + lhsSymbol.getId()
                                + " has been declared as " + lhsSymbolType + ". Cannot redeclare it as " + rhsSymbolType + ".");
                    }
                    ModuleAnalysis.hasErrors = true;
                    return true;
                }
        return false;
    }

    /**
     * Parses an assign whose rhs is ARRAYSIZE
     * @param rhsSymbol symbol on rhs
     * @param lhsSymbol symbol on lhs
     * @return true if is from type A = [VALUE] with A already declared as array or still not declared
     */
    private boolean parseArraySizeAssign(VarSymbol rhsSymbol, VarSymbol lhsSymbol) {
        if (rhsSymbol.getType().equals("ARRAYSIZE"))
        {
            if(lhsSymbol.getType().equals(SymbolType.ARRAY.toString()) || lhsSymbol.getType().equals(SymbolType.UNDEFINED.toString()))
            {
                lhsSymbol.setType(SymbolType.ARRAY.toString());
                lhsSymbol.setInitialized(true);
                addToSymbolTable(lhsSymbol);
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a symbol to the symbol table
     * @param lhsSymbol symbol on lhs
     */
    private void addToSymbolTable(VarSymbol lhsSymbol)
    {
        if ((inheritedSymbols.get(lhsSymbol.getId()) == null) && (mySymbols.get(lhsSymbol.getId()) == null))
        {
            mySymbols.put(lhsSymbol.getId(), lhsSymbol);
        }

    }

    /**
     * Gets the lhs variable
     * @param lhsTree node tree
     * @return lhs varSymbol
     */
    private VarSymbol getLhsVariable(SimpleNode lhsTree)
    {
        VarSymbol symbol = null;
        String id;
        Node child = lhsTree.jjtGetChild(0);
        switch (child.toString())
        {
            case "ARRAYACCESS":
                ASTARRAYACCESS astArrayAccess = (ASTARRAYACCESS) child;
                id = astArrayAccess.arrayID;
                symbol = (VarSymbol) hasAccessToSymbol(id);
                if (symbol == null)
                {
                    System.out.println("Line " + astArrayAccess.getBeginLine() + ": Variable " + id + " might not have been declared.");
                    return null;
                }

                if (!symbol.getType().equals(Type.ARRAY.toString()))
                {
                    System.out.println("Line " + astArrayAccess.getBeginLine() + ": Cannot access to an index of variable "
                            + symbol.getId() + " because it has type " + symbol.getType() + ".");
                    return null;
                }

                ASTINDEX astindex = (ASTINDEX) child.jjtGetChild(0);
                VarSymbol index = parseIndex(astindex);
                if (index == null)
                    return null;
                else if(index.isArrayAccess())
                {
                    VarSymbol newSymbol = symbol.getCopy();
                    newSymbol.setArrayAccess(true);
                    return newSymbol;
                }
                break;

            case "SCALARACCESS":
                id = ((ASTSCALARACCESS) child).id;
                symbol = (VarSymbol) hasAccessToSymbol(id);

                if (symbol == null)
                    symbol = new VarSymbol(id, SymbolType.UNDEFINED.toString(), false);

                break;
        }

        return symbol;
    }

    /**
     * Parses an array index
     * @param astIndex ASTINDEX
     * @return index varSymbol
     */
    private VarSymbol parseIndex(ASTINDEX astIndex)
    {
        String indexSymbolId = astIndex.indexID;
        if (indexSymbolId != null)
        {
            return (VarSymbol) checkSymbolExistsAndIsInitialized(astIndex, indexSymbolId);
        }
        else if(astIndex.indexValue != null)
        {
            VarSymbol symbol = new VarSymbol(astIndex.indexValue.toString(), Type.ARRAY.name(), true);
            symbol.setArrayAccess(true);
            return symbol;

        }

        return null;
    }

    /**
     * Parses an exprtest
     * @param astExprtest ASTEXPRTEST
     */
    void parseExprTest(ASTEXPRTEST astExprtest)
    {
        ASTLHS astLhs = (ASTLHS) astExprtest.jjtGetChild(0);
        VarSymbol lhsSymbol = parseLhs(astLhs);
        if (lhsSymbol == null)
            return;

        ASTRHS astRhs = (ASTRHS) astExprtest.jjtGetChild(1);
        VarSymbol rhsSymbol = parseRhs(astRhs);
        if (rhsSymbol == null)
            return;

        if (!lhsSymbol.getType().equals(rhsSymbol.getType()))
        {
            System.out.println("Line " + astLhs.getBeginLine() + ": Variables must have same type to be compared."
                    + "Variable " + lhsSymbol.getId() + " has type " + lhsSymbol.getType() + " and variable "
                    + rhsSymbol.getId() + " has type " + rhsSymbol.getType() + ".");
            ModuleAnalysis.hasErrors = true;
            return;
        }

        //if operands being tested are both array type and the comparison operator is not == or !=, semantic error.
        if (lhsSymbol.getType().equals(SymbolType.ARRAY.toString()))
        {
            if(!astExprtest.operation.equals("==") && !astExprtest.operation.equals("!="))
            {
                System.out.println("Line " + astLhs.getBeginLine() + ": Variables must be INTEGER to be compared. Variable "
                        + lhsSymbol.getId() + " has type " + lhsSymbol.getType() + " and variable " + rhsSymbol.getId()
                        + " has type " + rhsSymbol.getType() + ".");
                ModuleAnalysis.hasErrors = true;
            }
        }

    }

    /**
     * Parses the statements list
     * @param astStatements ASTSTATEMENTS
     */
    void parseStmtLst(ASTSTATEMENTS astStatements)
    {
        int statementsNumChilds = astStatements.jjtGetNumChildren();
        for (int i = 0; i < statementsNumChilds; i++)
        {
            SimpleNode node = (SimpleNode) astStatements.jjtGetChild(i);
            String nodeId = node.toString();
            switch (nodeId)
            {
                case "WHILE":
                    WhileAnalysis whileAnalysis = new WhileAnalysis(node, getUnifiedSymbolTable(), functionNameToFunctionSymbol);
                    whileAnalysis.parse();
                    mySymbols.putAll(whileAnalysis.mySymbols);
                    break;

                case "IF":
                    IfAnalysis ifAnalysis = new IfAnalysis(node, getUnifiedSymbolTable(), functionNameToFunctionSymbol);
                    ifAnalysis.parse();
                    mySymbols.putAll(ifAnalysis.mySymbols);
                    break;

                case "CALL":
                    parseCall((ASTCALL) node);
                    break;

                case "ASSIGN":
                    parseAssign((ASTASSIGN) node);
                    break;
            }
        }
    }

    /**
     * Sets all symbols as not initialized
     * @param symbols list of symbols
     * @return a list with symbols not initialized
     */
    HashMap<String, Symbol> setAllSymbolsAsNotInitialized(HashMap<String, Symbol> symbols)
    {
        HashMap<String, Symbol> symbolsNotInitialized = new HashMap<>();

        for (Entry<String, Symbol> o : symbols.entrySet())
        {
            String symbolName = o.getKey();
            VarSymbol symbol = (VarSymbol) o.getValue();
            symbol.setInitialized(false);
            symbolsNotInitialized.put(symbolName, symbol);
        }

        return symbolsNotInitialized;
    }

}
