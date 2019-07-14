package yal2jvm.hlir.liveness_analysis;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

/**
 * Class to represent a line of a method. While it is not guaranteed that one of
 * these lines matches a real line from the source code, in most cases there is
 * a direct correlation.
 */
public class Line
{
	private int id;
	private HashMap<String, Integer> varToBit;
	private BitSet use;
	private BitSet def;
	private BitSet in;
	private BitSet out;
	private ArrayList<Line> successors;
	private String label = "";
	private String jumpLabel = "";
	private boolean isJump = false;
	private boolean hasSuccessor = true;
	private String type;

	/**
	 *
	 * @param id
	 *            the line number
	 * @param varToBit
	 *            HashMap to map each of the method's variables into an integer
	 */
	public Line(int id, HashMap<String, Integer> varToBit)
	{
		this.id = id;
		this.varToBit = varToBit;
		this.use = new BitSet(this.varToBit.size());
		this.def = new BitSet(this.varToBit.size());
		this.in = new BitSet(this.varToBit.size());
		this.out = new BitSet(this.varToBit.size());
		this.successors = new ArrayList<>();
	}

	/**
	 * Returns a String representation of this line, including its USE, DEF, IN, OUT
	 * and SUCC sets
	 * 
	 * @return a String representation of the line
	 */
	@Override
	public String toString()
	{
		String s = "Line " + this.id + " (" + this.type + ") -> ";
		s += "USE: [" + stringifySet(this.use) + "] ";
		s += "DEF: [" + stringifySet(this.def) + "] ";
		s += "IN:  [" + stringifySet(this.in) + "] ";
		s += "OUT: [" + stringifySet(this.out) + "] ";
		s += "SUCC: [" + getSuccString() + "]";
		return s;
	}

	/**
	 * Gets a String representation of the SUCC set
	 * 
	 * @return a String representation of the SUCC set
	 */
	private String getSuccString()
	{
		String s = "";
		for (Line line : this.successors)
		{
			s += line.getId() + ", ";
		}
		if (s.length() > 0)
			s = s.substring(0, s.length() - 2);
		return s;
	}

	/**
	 * Stringifies a BitSet, translating the bits to their variable counterparts
	 * 
	 * @param set
	 *            the BitSet to stringiy
	 * @return a String representing the set
	 */
	private String stringifySet(BitSet set)
	{
		String s = "";
		for (String key : this.varToBit.keySet())
		{
			int i = varToBit.get(key);
			if (set.get(i))
				s += key + ", ";
		}
		if (s.length() > 0)
			s = s.substring(0, s.length() - 2);
		return s;
	}

	/**
	 * Adds a variable to the USE set
	 * 
	 * @param var
	 *            the variable to add
	 */
	public void addUse(String var)
	{
		int index = this.varToBit.get(var);
		this.use.set(index);
	}

	/**
	 * Adds a variable to the DEF set
	 * 
	 * @param var
	 *            the variable to add
	 */
	public void addDef(String var)
	{
		int index = this.varToBit.get(var);
		this.def.set(index);
	}

	/**
	 * Gets the line number
	 * 
	 * @return the line number
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Sets the line number
	 * 
	 * @param id
	 *            the line number
	 */
	public void setId(int id)
	{
		this.id = id;
	}

	/**
	 * Adds a label to this line
	 * 
	 * @param label
	 *            the label to add
	 */
	public void addLabel(String label)
	{
		this.label = label;
	}

	/**
	 * Gets the label of this line
	 * 
	 * @return the label of the line
	 */
	public String getLabel()
	{
		return this.label;
	}

	/**
	 * Adds a line as a successor to this one
	 * 
	 * @param line
	 *            the successor line
	 */
	public void addSuccessor(Line line)
	{
		this.successors.add(line);
	}

	/**
	 * Sets a flag about whether this line has a jump or not
	 * 
	 * @param b
	 *            the flag value
	 */
	public void setJump(boolean b)
	{
		this.isJump = b;
	}

	/**
	 * Gets the jump label of this line.
	 * 
	 * @return jump label if it has one, empty String otherwise
	 */
	public String getJumpLabel()
	{
		return jumpLabel;
	}

	/**
	 * Sets the jump label of this line
	 * 
	 * @param jumpLabel
	 *            the jump label to set
	 */
	public void setJumpLabel(String jumpLabel)
	{
		this.jumpLabel = jumpLabel;
	}

	/**
	 * Checks if the line has a jump
	 * 
	 * @return true if it has a jump, false otherwise
	 */
	public boolean isJump()
	{
		return this.isJump;
	}

	/**
	 * Gets all the successors of this line
	 * 
	 * @return a list with the successors of this line
	 */
	public ArrayList<Line> getSuccessors()
	{
		return successors;
	}

	/**
	 * Gets the USE set of this line
	 * 
	 * @return the USE set of this line
	 */
	public BitSet getUse()
	{
		return use;
	}

	/**
	 * Sets the USE set of this line
	 * 
	 * @param use
	 *            the USE set to set
	 */
	public void setUse(BitSet use)
	{
		this.use = use;
	}

	/**
	 * Gets the DEF set of this line
	 * 
	 * @return the DEF set of this line
	 */
	public BitSet getDef()
	{
		return def;
	}

	/**
	 * Sets the DEF set of this line
	 * 
	 * @param def
	 *            the DEF set to set
	 */
	public void setDef(BitSet def)
	{
		this.def = def;
	}

	/**
	 * Gets the IN set of this line
	 * 
	 * @return the IN set of this line
	 */
	public BitSet getIn()
	{
		return in;
	}

	/**
	 * Sets the IN set of this line
	 * 
	 * @param in
	 *            the IN set to set
	 */
	public void setIn(BitSet in)
	{
		this.in = in;
	}

	/**
	 * Gets the OUT set of this line
	 * 
	 * @return the OUT set of this line
	 */
	public BitSet getOut()
	{
		return out;
	}

	/**
	 * Sets the OUT set of this line
	 * 
	 * @param out
	 *            the OUT set to set
	 */
	public void setOut(BitSet out)
	{
		this.out = out;
	}

	/**
	 * Checks if this line has a successor
	 * 
	 * @return true if it has, false otherwise
	 */
	public boolean hasSuccessor()
	{
		return this.hasSuccessor;
	}

	/**
	 * Sets whether this line has a successor or not
	 * 
	 * @param hasSuccessor
	 *            true to set as having a successor, false otherwise
	 */
	public void setHasSuccessor(boolean hasSuccessor)
	{
		this.hasSuccessor = hasSuccessor;
	}

	/**
	 * Gets the type of line
	 * 
	 * @return the type of line
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * Sets the type of line
	 * 
	 * @param type
	 *            the type of line to set
	 */
	public void setType(String type)
	{
		this.type = type;
	}
}
