package yal2jvm.hlir.liveness_analysis;

/**
 * Class to represent an interference between two variables
 */
public class IntPair
{
	private String var1;
	private String var2;

	/**
	 * Constructor
	 * 
	 * @param var1
	 *            name of the first variable
	 * @param var2
	 *            name of the second variable
	 */
	public IntPair(String var1, String var2)
	{
		this.var1 = var1;
		this.var2 = var2;
	}

	/**
	 * Gets the first variabe
	 * 
	 * @return first variable
	 */
	public String getVar1()
	{
		return var1;
	}

	/**
	 * Sets the first variable
	 * 
	 * @param var1
	 *            first variable
	 */
	public void setVar1(String var1)
	{
		this.var1 = var1;
	}

	/**
	 * Gets the second variabe
	 * 
	 * @return second variable
	 */
	public String getVar2()
	{
		return var2;
	}

	/**
	 * Sets the second variable
	 * 
	 * @param var2
	 *            second variable
	 */
	public void setVar2(String var2)
	{
		this.var2 = var2;
	}

	/**
	 *
	 * @return
	 */
	@Override
	public String toString()
	{
		return var1 + "-" + var2;
	}
}
