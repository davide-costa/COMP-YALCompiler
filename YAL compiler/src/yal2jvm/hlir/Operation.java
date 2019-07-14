package yal2jvm.hlir;

public enum Operation
{
	ADD, SUB, MULT, DIV, SHIFT_R, SHIFT_L, USHIFT_R, AND, OR, XOR;

	/**
	 * Receives an operator as a String and return the equivalent Operation enum
	 * value
	 * 
	 * @param operator
	 *            operator that will be used to retrieve it's Operation enum
	 *            equivalent
	 * @return Operation enum value if operator is recognized. Null if it isn't.
	 */
	public static Operation parseOperator(String operator)
	{
		switch (operator)
		{
		case "+":
			return ADD;
		case "-":
			return SUB;
		case "*":
			return MULT;
		case "/":
			return DIV;
		case ">>":
			return SHIFT_R;
		case "<<":
			return SHIFT_L;
		case ">>>":
			return USHIFT_R;
		case "&":
			return AND;
		case "|":
			return OR;
		case "^":
			return XOR;
		}

		return null;
	}

	/**
	 * Retrieves this object's symbol as a String operator
	 *
	 * @return this object as a String operator. Empty string if this object isn't a
	 *         valid operator.
	 */
	String getSymbol()
	{
		switch (this)
		{
		case ADD:
			return "+";
		case SUB:
			return "-";
		case MULT:
			return "*";
		case DIV:
			return "/";
		case SHIFT_R:
			return ">>";
		case SHIFT_L:
			return "<<";
		case USHIFT_R:
			return ">>>";
		case AND:
			return "&";
		case OR:
			return "|";
		case XOR:
			return "^";
		}

		return "";
	}
}