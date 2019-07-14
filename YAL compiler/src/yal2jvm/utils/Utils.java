package yal2jvm.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import yal2jvm.hlir.Operation;
import yal2jvm.symbol_tables.Symbol;

/**
 * Class containing useful functions
 */
public class Utils
{
	/**
	 * Receives an HashMap of key String and value Symbol and returns a copy of it
	 *
	 * @param original
	 *            original HashMap to be copied
	 * @return HashMap that is a copy of the parameter
	 */
	public static HashMap<String, Symbol> copyHashMap(HashMap<String, Symbol> original)
	{
		HashMap<String, Symbol> copy = new HashMap<>();
		for (Map.Entry<String, Symbol> entry : original.entrySet())
			copy.put(entry.getKey(), entry.getValue().getCopy());

		return copy;
	}

	/**
	 * Checks whether or not the character parameter is the last character of the
	 * String string
	 *
	 * @param character
	 *            used to check if it is the last character of string
	 * @param string
	 *            the string used to check for the character
	 * @return true if the last character of string is equal to character. False if
	 *         it isn't.
	 */
	public static boolean isLastCharacterOfString(String character, String string)
	{
		return string.lastIndexOf(character) == string.length() - 1;
	}

	/**
	 * Searches an array to see if it contains the String string
	 *
	 * @param array
	 *            the array that will be searched
	 * @param string
	 *            the string that will be searched in array
	 * @return index of the string in the array. -1 if the array doesn't contain the
	 *         string.
	 */
	public static int stringArrayContains(String[] array, String string)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (array[i].equals(string))
				return i;
		}

		return -1;
	}

	/**
	 * Searches an array for a regex expression match
	 * 
	 * @param array
	 *            the array that will be searched
	 * @param regex
	 *            the regex expression that will be searched in the array
	 * @return index of the regex expression in the array. -1 if the array doesn't
	 *         match the regex expression.
	 */
	public static int stringArrayMatches(String[] array, String regex)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (array[i].matches(regex))
				return i;
		}

		return -1;
	}

	/**
	 * Searches an array for the first time it doesn't match a regex expression
	 * 
	 * @param array
	 *            the array that will be searched
	 * @param regex
	 *            the regex expression that will be searched in the array
	 * @return index of the first occurrence where the regex expression doesn't
	 *         match in the array. -1 if the regex expression matches all elements
	 *         of the array.
	 */
	public static int stringArrayNotMatches(String[] array, String regex)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (!array[i].matches(regex))
				return i;
		}

		return -1;
	}

	/**
	 * Receives two string variables and the operator between them and checks which
	 * operator the string is. After finding out which operator it is, it is
	 * returned the value of the operation between the integer of the two string
	 * variables
	 * 
	 * @param var1
	 *            left side variable
	 * @param var2
	 *            right side variable
	 * @param stringOperator
	 *            operator between the two variables
	 * @return the result of the operation between the variables. 0 if the operator
	 *         isn't recognized.
	 */
	public static int getOperationValue(String var1, String var2, String stringOperator)
	{
		Operation operator = Operation.parseOperator(stringOperator);
		assert operator != null;
		return getOperationValueByOperator(var1, var2, operator);
	}

	/**
	 * Receives two string variables and the operator, enum from class Operation,
	 * between them and checks which operator the string is. After finding out which
	 * operator it is, it is returned the value of the operation between the integer
	 * of the two string variables
	 * 
	 * @param var1
	 *            left side variable
	 * @param var2
	 *            right side variable
	 * @param operator
	 *            operator between the two variables, of class Operation
	 * @return the result of the operation between the variables. 0 if the operator
	 *         isn't recognized.
	 */
	public static int getOperationValueByOperator(String var1, String var2, Operation operator)
	{
		switch (operator)
		{
		case ADD:
			return Integer.parseInt(var1) + Integer.parseInt(var2);
		case SUB:
			return Integer.parseInt(var1) - Integer.parseInt(var2);
		case MULT:
			return Integer.parseInt(var1) * Integer.parseInt(var2);
		case DIV:
			return Integer.parseInt(var1) / Integer.parseInt(var2);
		case SHIFT_R:
			return Integer.parseInt(var1) >> Integer.parseInt(var2);
		case SHIFT_L:
			return Integer.parseInt(var1) << Integer.parseInt(var2);
		case USHIFT_R:
			return Integer.parseInt(var1) >>> Integer.parseInt(var2);
		case AND:
			return Integer.parseInt(var1) & Integer.parseInt(var2);
		case OR:
			return Integer.parseInt(var1) | Integer.parseInt(var2);
		case XOR:
			return Integer.parseInt(var1) ^ Integer.parseInt(var2);
		}

		return 0;
	}

	/**
	 * Receives a TreeSet and copies its elements to an arrayList and returns it.
	 *
	 * @param set
	 *            the TreeSet that will be copied
	 * @param <T>
	 *            elements of collection T
	 * @return new ArrayList with the elements of set
	 */
	public static <T> ArrayList<T> setToList(TreeSet<T> set)
	{
		return new ArrayList<>(set);
	}
}
