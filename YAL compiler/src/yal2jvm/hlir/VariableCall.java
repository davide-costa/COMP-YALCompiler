package yal2jvm.hlir;

/**
 * Variable Call
 */
public class VariableCall extends Variable
{
	private IRCall irCall;

	/**
	 * VariableCall constructor
	 * @param var variable name
	 * @param type variable type
	 * @param irCall IRCall
	 */
	VariableCall(String var, Type type, IRCall irCall)
	{
		super(var, type);
		this.irCall = irCall;
	}

	/**
	 * Returns the value of the field irCall
	 * @return	value of the field irCall
	 */
	IRCall getIrCall()
	{
		return irCall;
	}
}
