package yal2jvm.hlir;

import java.util.ArrayList;

/**
 * IRAllocate class that extends the IRNode class
 */
public class IRAllocate extends IRNode
{
    private String name;
    private IRNode lhsIndex = null;
    private Type type;
    private IRNode rhs;
    private int register = -1;
	private boolean storeVarGlobal = false;
	private IRGlobal global;

    /**
     * Constructor for cases when we have a lhs variable, previously allocated or not, and an integer or a variable on rhs.
     * Cases like a=1 or a=b.
     * @param name of the lhs variable
     * @param value of the rhs, constant or variable
     */
    public IRAllocate(String name, Variable value)
    {
        this.type = Type.INTEGER;
        this.setNodeType("Allocate");
        this.name = name;

        if(value.getType().equals(Type.INTEGER))
            this.rhs = new IRConstant(value.getVar());
        else
            this.rhs = new IRLoad(value);

        this.addChild(this.rhs);
    }

    /**
     * Constructor for cases when we have a lhs variable, previously allocated or not, and an integer or a variable on rhs indicating array size.
     * Cases like a = [5].
     * @param name the lhs variable
     * @param value the array size
     * @param arraySize indicate Type.ARRAYSIZE, It's mandatory
     */
    public IRAllocate(String name, Variable value, Type arraySize)
    {
        assert arraySize == Type.ARRAYSIZE;

        this.setNodeType("Allocate");
        this.name = name;
        this.type = Type.ARRAYSIZE;

        if(value.getType().equals(Type.INTEGER))
            this.rhs = new IRConstant(value.getVar());
        else
            this.rhs = new IRLoad(value);
        this.addChild(this.rhs);
    }

    /**
     * Constructor for cases when we have a lhs variable array, previously allocated, and set an element of the array.
     * The element set is at an index, as is set with an integer or a variable on rhs.
     * Cases like a[i] = 5.
     * @param name the lhs variable, an array at a given index
     * @param value the rhs value
     */
    public IRAllocate(VariableArray name, Variable value)
    {
        this.type = Type.ARRAY;
        this.setNodeType("Allocate");
        this.name = name.getVar();

        Variable at = name.getAt();
        if(at.getType().equals(Type.INTEGER))
            this.lhsIndex = new IRConstant(at.getVar());
        else
            this.lhsIndex = new IRLoad(at);
        this.addChild(this.lhsIndex);

        if(value.getType().equals(Type.INTEGER))
            this.rhs = new IRConstant(value.getVar());
        else
            this.rhs = new IRLoad(value);
        this.addChild(this.rhs);
    }

    /**
     * Constructor for cases when we have a lhs variable integer, previously allocated or not, and set its value with an array element.
     * The element is at an index, as it's value is set to the lhs variable.
     * Cases like a = b[5].
     * @param name the lhs variable
     * @param value the rhs value, an array at a given index
     */
    public IRAllocate(Variable name, VariableArray value)
    {
        this.setNodeType("Allocate");
        this.name = name.getVar();
        this.type = Type.INTEGER;

        this.rhs = new IRLoad(value);
        this.addChild(this.rhs);
    }

    /**
     * Constructor for cases when we have a lhs variable array, previously allocated, and set an element of the array.
     * The element is at an index, as it's value is set to the lhs variable at another index because rhs is also an array.
     * Cases like a[i] = b[5].
     * @param name the lhs variable, an array at a given index
     * @param value the rhs variable, an array at a given index
     */
    public IRAllocate(VariableArray name, VariableArray value)
    {
        this.setNodeType("Allocate");
        this.name = name.getVar();
        this.type = Type.ARRAY;

        Variable at = name.getAt();
        if(at.getType().equals(Type.INTEGER))
            this.lhsIndex = new IRConstant(at.getVar());
        else
            this.lhsIndex = new IRLoad(at);
        this.addChild(this.lhsIndex);

        this.rhs = new IRLoad(value);
        this.addChild(this.rhs);
    }

    /**
     * Returns the value of field type
     * @return value of field type
     */
    public Type getType()
    {
        return type;
    }

    /**
     * Returns the value of field rhs
     * @return value of field rhs
     */
    public IRNode getRhs()
    {
        return rhs;
    }

    /**
     * Gets the instructions for code generation
     * @return instructions list
     */
    @Override
    public ArrayList<String> getInstructions()
    {
        ArrayList<String> inst = new ArrayList<>();

        handleConstantRhsForConstantPropagationOptimisation();

        IRNode node = getVarIfExists(name);
        if(node == null)
        {
            initRegister();
        }
        else if(node instanceof IRGlobal)
        {
            global = (IRGlobal) node;
            this.storeVarGlobal  = true;
        }
        else if(node instanceof IRArgument)
            register = ((IRArgument)node).getRegister();
        else
            register = ((IRAllocate) node).getRegister();

        if(type == Type.ARRAYSIZE)
        {
            inst.addAll(rhs.getInstructions());
            inst.add("newarray int");
        }

        //get store instructions
        inst.addAll(getStoreInst());

        return inst;
    }

    /**
     * handles right hand side constant for constant propagation optimization
     */
    private void handleConstantRhsForConstantPropagationOptimisation()
    {
        IRMethod method = (IRMethod) findParent("Method");

        //do nothing if variable type is array and we are setting all its position to the value in rhs
        if(isRhsArrayAndLhsInteger(method))
            return;

        String varName = getVarNameForConstantName(name, lhsIndex);
        if(rhs instanceof IRConstant)
        {
            if(type == Type.INTEGER || type == Type.ARRAY)
                method.addToConstVarNameToConstValue(varName, (IRConstant) rhs);
        }
        else
        {
            IRLoad load = (IRLoad)rhs;
            String rhsName = getVarNameForConstantName(load.getName(), load.getIndex());
            if(method.getConstValueByConstVarName(rhsName) != null)
            {
                rhs = new IRConstant(method.getConstValueByConstVarName(rhsName).getValue());
                rhs.setParent(load.parent);
                method.addToConstVarNameToConstValue(varName, (IRConstant) rhs);
            }
            else
                method.removeFromConstVarNameToConstValue(varName);
        }
    }

    /**
     * Checks if Rhs is Array And Lhs is Integer
     * @param method    method of the var to check
     * @return          True if the validation is confirmed. False if not.
     */
    private boolean isRhsArrayAndLhsInteger(IRMethod method)
    {
        String varType = getVarType(method);
        return varType.equals(Type.ARRAY.name()) && type == Type.INTEGER;
    }

    /**
     * get the instructions to store the variable allocated
     * @return list instructions to store
     */
    private ArrayList<String> getStoreInst()
	{
        ArrayList<String> inst = new ArrayList<>();
		if (this.storeVarGlobal)
		{
		    String typeStr = type.name();
            if(typeStr != null && (global.getType() == Type.VARIABLE || global.getType() == Type.INTEGER)) // i = 5;
            {
                inst.addAll(rhs.getInstructions());
                inst.add(getInstructionToStoreGlobal(new Variable(name, type)));
                return inst;
            }

            if(typeStr == null)
                typeStr = rhs.getNodeType();

            if(rhs.parent.getNodeType().equals("Allocate")) {
                IRAllocate rhsParent = (IRAllocate) rhs.parent;
                if(rhsParent.type == Type.ARRAYSIZE) {
                    inst.add(getInstructionToStoreGlobal(new Variable(name, type))); // i = [5];
                    type = Type.ARRAY;
                    return inst;
                }
            }

            Type prevType = global.getType();
            if(prevType == Type.ARRAY && type == Type.INTEGER)
                typeStr = Type.ARRAY.name();

            if(typeStr.equals(Type.ARRAY.name()) && lhsIndex == null)
                inst.addAll(setAllArrayElements()); // i = 5; com i array
            else
            {
                if(lhsIndex != null) // a[i] = 5;
                    inst.addAll(setGlobalArrayElementByIRNode(lhsIndex, new Variable(name, type), rhs));
            }

		}
		else
		{
            IRMethod method = (IRMethod) findParent("Method");
            String varType = getVarType(method);

            if(varType != null && varType.equals(Type.INTEGER.name())) // i = 5;
            {
                inst.addAll(rhs.getInstructions());
                inst.add(getInstructionToStoreIntInRegister(this.register));

                //this is done after getInstructions of rhs, because loadConstant is set there
                //this puts in the hashMap the new value of the variable name or replace it
                String varName = getVarNameForConstantName(name, lhsIndex);

                if(rhs instanceof IRConstant)
                    method.addToConstVarNameToConstValue(varName, (IRConstant) rhs);
                else
                {
                    String value = ((IRLoad)rhs).getLoadedConstantValue();
                    if(value != null)
                        method.addToConstVarNameToConstValue(varName, new IRConstant(value));
                    else
                        method.removeFromConstVarNameToConstValue(varName);
                }

                return inst;
            }

            if(varType == null)
                varType = rhs.getNodeType();

            if(rhs.parent != null && rhs.parent.getNodeType().equals("Allocate"))
            {
                IRAllocate rhsParent = (IRAllocate) rhs.parent;
                if(rhsParent.type == Type.ARRAYSIZE)
                {
                    inst.add(getInstructionToStoreArrayInRegister(this.register)); // i = [5];
                    type = Type.ARRAY;
                    return inst;
                }
            }

            if(varType.equals(Type.ARRAY.name()) && lhsIndex == null)
                inst.addAll(setAllArrayElements()); // i = 5; com i array
            else
            {
                if(lhsIndex != null) // a[i] = 5;
                    inst.addAll(setLocalArrayElementByIRNode(lhsIndex, register, rhs));
            }
		}

        return inst;
	}

    /**
     * get var with name (attribute) type
     * @param method method of the var to check
     * @return string with var type
     */
    private String getVarType(IRMethod method)
    {
        String varType;
        IRNode node = getVarIfExists(name);
        if(node instanceof IRAllocate)
            varType = ((IRAllocate)node).getType().name();
        else if(node instanceof IRGlobal)
            varType = ((IRGlobal)node).getType().name();
         else
            varType = method.getArgumentType(name).name();

        return varType;
    }

    /**
     * set all array elements
     * @return list instructions to set all array elements
     */
    private ArrayList<String> setAllArrayElements()
    {
        int reg = -1;
        IRNode node = getVarIfExists(name);
        if(node == null)
        {
            System.out.println("Internal error! The program will be closed.");
            System.exit(-1);
        }
        else if(node instanceof IRArgument)
            reg = ((IRArgument)node).getRegister();
        else if(node instanceof IRAllocate)
            reg = ((IRAllocate) node).getRegister();

        String arrayRefJVMCode;
        if(storeVarGlobal)
        {
            Type prevType = global.getType();
            if(prevType == Type.ARRAY && type == Type.INTEGER)
                type = Type.ARRAY;
            arrayRefJVMCode = getInstructionToLoadGlobalToStack(new Variable(name, type));
        }
        else
            arrayRefJVMCode = getInstructionToLoadArrayFromRegisterToStack(reg);
        ArrayList<String> valueJVMCode = rhs.getInstructions();
        return getCodeForSetAllArrayElements(arrayRefJVMCode, valueJVMCode);
    }

    /**
     * init register for variable
     */
    private void initRegister()
    {
        if (!this.storeVarGlobal && this.register == -1)
        {
            this.register = ((IRMethod) parent).getRegN();
            ((IRMethod) parent).incrementRegN();
        }
    }

    /**
     * get register of the variable
     * @return the register number
     */
    public int getRegister()
    {
        IRMethod method = (IRMethod) findParent("Method");
        Integer registerNumber = HLIR.allocatedRegisterByMethodName.get(method.getName()).get(name);
        if(registerNumber == null)
            return -1;
        else
            return registerNumber;
    }

    /**
     * Sets the value of the field register to the value of the parameter register
     * @param register  register number
     */
    public void setRegister(int register)
    {
    	if (!this.storeVarGlobal)
    		this.register = register;
    }

    /**
     * Returns the value of field name
     * @return  value of field name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the value of field name to the value of the parameter name
     * @param name  new value of the field name
     */
    public void setName(String name)
    {
        this.name = name;
    }
}
