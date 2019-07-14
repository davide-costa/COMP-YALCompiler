package yal2jvm.ast;

public class ErrorCounter
{

	private int noErrors = 0;

	public ErrorCounter()
	{

	}

	public boolean errorControl()
	{
		noErrors++;

		return noErrors == 10;
	}

	public int getNoErrors()
	{
		return noErrors;
	}
}
