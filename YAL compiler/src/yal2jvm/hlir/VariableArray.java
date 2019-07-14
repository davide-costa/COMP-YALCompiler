package yal2jvm.hlir;

/**
 * VariableArray
 */
class VariableArray extends Variable
{
	private Variable at;

	/**
	 * VariableArray constructor
	 * @param var variable name
	 * @param at variable index
	 */
	VariableArray(String var, Variable at)
	{
		super(var, Type.ARRAY);
		this.at = at;
	}

	/**
	 * Returns the value of the field at
	 * @return	value of the field at
	 */
	Variable getAt()
	{
		return at;
	}
}
