package yal2jvm.hlir;

/**
 * Variable
 */
public class Variable
{
	private String var = null;
	private boolean sizeAccess = false;
	private Type type;

	/**
	 * Variable constructor
	 * @param var variable name
	 * @param type variable type
	 */
	Variable(String var, Type type)
	{
		this.type = type;

		if (var != null) /* var can be null in case of CALL type */
		{
			if (var.contains(".size"))
			{
				sizeAccess = true;
				this.var = var.split(".size")[0];
			} else
			{
				this.var = var;
			}
		}
	}

	/**
	 * Sets the value of the field type to the value of the parameter type
	 * @param type new value for the field type
	 */
	public void setType(Type type)
	{
		this.type = type;
	}

	/**
	 * Returns the value of the field sizeAccess
	 * @return value of the field sizeAccess
	 */
	public boolean isSizeAccess()
	{
		return sizeAccess;
	}

	/**
	 * Returns the value of the field type
	 * @return value of the field type
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * Returns the value of the field var
	 * @return value of the field var
	 */
	public String getVar()
	{
		return var;
	}
}
