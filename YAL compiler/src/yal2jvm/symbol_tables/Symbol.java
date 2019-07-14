package yal2jvm.symbol_tables;

import java.util.Objects;

/**
 * Symbol class that is extended by the other symbol classes
 */
public class Symbol
{

	protected String id;

	/**
	 * Constructor for class Symbol
	 * 
	 * @param id
	 *            id for new object of class Symbol
	 */
	public Symbol(String id)
	{
		this.id = id;
	}

	/**
	 * Returns this object's id
	 * 
	 * @return object id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Sets this object's id equal to the parameter
	 * 
	 * @param id
	 *            new id for this object
	 */
	public void setId(String id)
	{
		this.id = id;
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
		return id.equals(((Symbol) other).getId());
	}

	/**
	 * Creates a copy of this object
	 * 
	 * @return new Symbol object
	 */
	public Symbol getCopy()
	{
		return new Symbol(new String(id));
	}

	/**
	 * Hashes the id of this object
	 * 
	 * @return the value of the hash
	 */
	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}
}
