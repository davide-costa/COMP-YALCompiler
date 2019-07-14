package yal2jvm.symbol_tables;

/**
 * VarSymbol class that extends the class Symbol
 */
public class VarSymbol extends Symbol
{

	private String type;
	private boolean initialized;
	private boolean arrayAccess;

	/**
	 * Constructor for the class VarSymbol
	 * 
	 * @param id
	 *            id for the new object
	 * @param type
	 *            type of the new object
	 * @param initialized
	 *            sets the initialized field to true or false
	 */
	public VarSymbol(String id, String type, boolean initialized)
	{
		super(id);
		this.type = type;
		this.initialized = initialized;
	}

	/**
	 * Returns the value of the field type
	 * 
	 * @return value of field type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * Sets the field type to the parameter type's value
	 * 
	 * @param type
	 *            new value for the field type
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * Returns the value of the field initialized
	 * 
	 * @return boolean value of the field initialized
	 */
	public boolean isInitialized()
	{
		return initialized;
	}

	/**
	 * Sets the field initialized to the parameter initialized's value
	 * 
	 * @param initialized
	 *            new value for field initialized
	 */
	public void setInitialized(boolean initialized)
	{
		this.initialized = initialized;
	}

	/**
	 * Returns the value of the field arrayAccess
	 *
	 * @return boolean value of the field arrayAccess
	 */
	public boolean isArrayAccess()
	{
		return arrayAccess;
	}

	/**
	 * Sets the field arrayAccess to the parameter arrayAccess's value
	 *
	 * @param arrayAccess
	 *            new value for field initialized
	 */
	public void setArrayAccess(boolean arrayAccess)
	{
		this.arrayAccess = arrayAccess;
	}

	/**
	 * Hashes the id of this object
	 * 
	 * @return the value of the hash
	 */
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}

	/**
	 * Checks if this object is equal to the object in the parameter
	 * 
	 * @param other
	 *            the object that will be compared to this object
	 * @return true if the objects have the same id. False if not.
	 */
	@Override
	public boolean equals(Object other)
	{
		return id.equals(((VarSymbol) other).getId());
	}

	/**
	 * Creates a copy of this object
	 * 
	 * @return new VarSymbol object
	 */
	public VarSymbol getCopy()
	{
		return new VarSymbol(new String(id), new String(type), new Boolean(initialized));
	}

	/**
	 * Uses the getCopy method
	 * 
	 * @return getCopy method
	 */
	@Override
	protected Object clone()
	{
		return getCopy();
	}
}
