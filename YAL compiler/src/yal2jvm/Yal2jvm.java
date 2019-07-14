package yal2jvm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import yal2jvm.ast.*;
import yal2jvm.hlir.HLIR;
import yal2jvm.semantic_analysis.ModuleAnalysis;
import yal2jvm.utils.Utils;

/**
 * Yal2jvm class that reads the console's arguments and calls the compiler's main
 * functions
 */
public class Yal2jvm
{
	private static final int MAX_LOCAL_VARS = 255;
	public static boolean VERBOSE = false;
	public static String moduleName;

	private int localVars;
	private boolean optimize;
	private boolean keepJFile;
	private String inputFile;
	private SimpleNode ast;

	/**
	 * Constructor for the class Yal2jvm, receives as parameters all the possible
	 * inputs from user
	 *
	 * @param localVars
	 *            number of locals to use in register allocation
	 * @param optimize
	 *            boolean indicating if it is to apply optimizations or not
	 * @param keepJFile
	 *            boolean indicating if it is to keep .j generated file
	 * @param verbose
	 *            boolean indicating if it is to show program logs and compiler steps
	 *            information
	 * @param inputFile
	 *            path to the file to be compiled
	 */
	public Yal2jvm(int localVars, boolean optimize, boolean keepJFile, boolean verbose, String inputFile)
	{
		this.localVars = localVars;
		this.optimize = optimize;
		this.keepJFile = keepJFile;
		Yal2jvm.VERBOSE = verbose;
		this.inputFile = inputFile;
	}

	/**
	 * Parses the program's arguments. Creates and runs an instance of the compiler using
	 * those arguments.
	 * 
	 * @param args
	 *            list of arguments
	 */
	public static void main(String args[])
	{
		String inputFile = null;
		boolean optimize = false;
		boolean keepJFile = false;
		boolean verbose = false;
		int localVars = MAX_LOCAL_VARS;
		boolean validInput = true;

		if (args.length == 0)
			validInput = false;

		if (Utils.stringArrayContains(args, "-o") != -1)
			optimize = true;

		if (Utils.stringArrayContains(args, "-S") != -1)
			keepJFile = true;

		if (Utils.stringArrayContains(args, "-v") != -1)
			verbose = true;

		if (Utils.stringArrayContains(args, "-help") != -1)
		{
			printUsage();
			System.exit(-1);
		}

		String regexForNumberBetween0And255 = "\\b(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\b";
		int registersValueIndex = Utils.stringArrayMatches(args, "-r=" + regexForNumberBetween0And255);
		if (registersValueIndex != -1)
		{
			String localVarsString = args[registersValueIndex].split("=")[1];
			localVars = Integer.parseInt(localVarsString);
			if (localVars == 0)
				localVars = MAX_LOCAL_VARS;
		}

		String regexForFlag = "-r=" + regexForNumberBetween0And255 + "|-o|-S|-v";
		int inputFileIndex;
		if ((inputFileIndex = Utils.stringArrayNotMatches(args, regexForFlag)) != -1)
			inputFile = args[inputFileIndex];

		if (inputFile != null && validInput)
			validInput = inputFile.toLowerCase().endsWith(".yal");

		if (!validInput || inputFile == null)
		{
			System.out.println("\nInsufficient or incorrect arguments for the Yal2jvm compiler");
			printUsage();
			System.exit(-5);
		} else
		{
			Yal2jvm instance = new Yal2jvm(localVars, optimize, keepJFile, verbose, inputFile);
			instance.run();
		}
	}

	/**
	 * This method runs all the necessary steps to compile the file, converting Yal code
	 * into JVM bytecode
	 *
	 */
	public void run()
	{
		if (VERBOSE)
			runWithLogging();

		FileInputStream inputStream = getFileStream();

		syntacticAnalysis(inputStream);
		semanticAnalysis();

		HLIR hlir = createHLIR();
		registerAllocation(hlir);
		String moduleName = instructionSelection(hlir);

		compileToBytecode(moduleName + ".j");

		System.exit(0);
	}

	/**
	 * This method runs all the necessary steps to compile the file, converting Yal code
	 * into JVM bytecode. Same as the run() method, but it outputs logging info about the compilation process.
	 */
	private void runWithLogging()
	{
		log("-----------------------------------------------------------------");
		log("Starting compilation of Yal file " + inputFile + " with the following options:");
		log("Max number of regs: " + localVars);
		log("Optimizations:      " + optimize);
		log("Keep Jasmin file:   " + keepJFile);
		log("Verbose output:     " + VERBOSE + "\n");

		FileInputStream inputStream = getFileStream();
		log("-----------------------------------------------------------------");

		log("Initiating lexical and syntactic analysis\n");
		syntacticAnalysis(inputStream);

		log("AST generated by syntactic analysis:\n");
		if (VERBOSE)
		{
			ast.dump("");
			System.out.println();
		}

		log("Completed lexical and syntactic analysis");

		log("-----------------------------------------------------------------");

		log("Initiating semantic analysis");
		semanticAnalysis();
		log("Completed semantic analysis");

		log("-----------------------------------------------------------------");

		log("Initiating HLIR generation");
		HLIR hlir = createHLIR();
		log("Completed HLIR generation");

		log("-----------------------------------------------------------------");

		log("Initiating register allocation");
		registerAllocation(hlir);
		log("Completed register allocation");

		log("-----------------------------------------------------------------");

		log("Initiating instruction selection" + (this.optimize ? " with HLIR optimizations" : ""));
		String moduleName = instructionSelection(hlir);
		log("Completed instruction selection" + (this.optimize ? " with HLIR optimizations" : ""));

		log("-----------------------------------------------------------------");

		log("Initiating compilation of instructions into JVM bytecode");
		compileToBytecode(moduleName + ".j");
		log("Completed compilation of instructions into JVM bytecode");

		log("-----------------------------------------------------------------");

		System.exit(0);
	}

	/**
	 * This method does syntactic Analysis and creates AST, using createAst method
	 * It terminates the compiler with error code -2 if errors found.
	 * 
	 * @param inputStream
	 *            FileInputStream object to the file .yal to be analysed
	 */
	private void syntacticAnalysis(FileInputStream inputStream)
	{
		ast = createAst(inputStream);
		if (ast == null)
			System.exit(-2);
	}

	/**
	 * This method does semantic Analysis. It terminates the compiler with error
	 * code -3 if errors found.
	 */
	private void semanticAnalysis()
	{
		ModuleAnalysis moduleAnalysis = new ModuleAnalysis(ast);
		moduleAnalysis.parse();
		if (ModuleAnalysis.hasErrors)
			System.exit(-3);
	}

	/**
	 * This method creates HLIR (High level intermediate representation), using the
	 * ast produced by syntactic analysis It terminates the compiler with error code
	 * -2 if errors found.
	 * 
	 * @return the created HLIR, object from class HLIR
	 */
	private HLIR createHLIR()
	{
		HLIR hlir = new HLIR(ast);
		if (VERBOSE)
			hlir.dumpIR();

		if (this.optimize)
			hlir.setOptimize();
		return hlir;
	}

	/**
	 * This method does register allocation using data flow analysis and graph
	 * coloring It terminates the compiler with error code -6 if errors found.
	 * 
	 * @param hlir
	 *            HLIR (High level intermediate representation)
	 */
	private void registerAllocation(HLIR hlir)
	{
		hlir.dataflowAnalysis();
		boolean allocated = hlir.allocateRegisters(this.localVars);
		if (!allocated)
			System.exit(-6);
	}

	/**
	 * This method does instructions selection, getting from the HLIR the jvm code
	 * for the file. It starts at the root, and recursively gets all the
	 * instructions. It also save the instructions on the jasmin (.j) file.
	 * 
	 * @param hlir
	 *            HLIR (High level intermediate representation) from which the jvm
	 *            code will be generated
	 * @return module name of the compiled file
	 */
	private String instructionSelection(HLIR hlir)
	{
		ArrayList<String> instructions = hlir.selectInstructions();
		String moduleName = hlir.getModuleName();
		saveToJasminFile(instructions, moduleName);
		return moduleName;
	}

	/**
	 * This method prints the usage message, shown when parameters to the compiler
	 * are incorrect or insufficient
	 */
	private static void printUsage()
	{
		System.out.println("\nUsage:\tjava -jar yal2jvm.jar [-r=<0..255>] [-o] [-S] [-v] [-help] <input_file.yal>\n");
		System.out.println("\t-r=<0..255>       number of JVM local vars per function (default 255)  (optional)");
		System.out.println("\t-o                run three additional code optimizations              (optional)");
		System.out.println("\t-S                keep the intermediate Jasmin file (.j) on the CWD    (optional)");
		System.out.println("\t-v                allow verbose output of all compilation stages       (optional)");
		System.out.println("\t-help             prints this help and ignores all other options       (optional)");
		System.out.println("\t<input_file>.yal  path to the .yal file to compile                     (mandatory)");
	}

	/**
	 * This method creates the FileInputStream object to the file to compile
	 * 
	 * @return FileInputStream object to the input file
	 */
	private FileInputStream getFileStream()
	{
		FileInputStream inputStream = null;
		try
		{
			inputStream = new FileInputStream(inputFile);
		} catch (FileNotFoundException e)
		{
			System.out.println("Error: file " + inputFile + " not found.\n");
			System.exit(-4);
		}
		return inputStream;
	}

	/**
	 * This method executes the syntatic analysis using YalParser, creating the AST.
	 * It prints some errors according to syntactic analysis results.
	 * 
	 * @param inputStream
	 *            FileInputStream object of the file to be read and compiled
	 * @return root node of the ast. The all AST.
	 */
	private SimpleNode createAst(FileInputStream inputStream)
	{
		new YalParser(inputStream);
		SimpleNode root = null;
		try
		{
			root = YalParser.Module();

			int noErrors = YalParser.errorCounter.getNoErrors();
			if (noErrors > 0)
			{
				if (noErrors >= 10)
					System.err.println("At least 10 errors found!");
				else
					System.err.println(noErrors + " errors found!");

				return null;
			}
		} catch (ParseException e)
		{
			System.out.println("Error: fatal error during parsing stage\n");
			System.exit(-2);
		}

		return root;
	}

	/**
	 * This method saves the instructions of jvm code generated by the compiler in a
	 * file with name = moduleName.j, in the CWD
	 * 
	 * @param instructions
	 *            instructions of jvm code generated by the compiler
	 * @param moduleName
	 *            name of the module that was compiled
	 */
	private void saveToJasminFile(ArrayList<String> instructions, String moduleName)
	{
		try
		{
			BufferedWriter file = new BufferedWriter(new FileWriter(moduleName + ".j"));

			for (int i = 0; i < instructions.size(); i++)
			{
				file.write(instructions.get(i));
				file.write("\n");
			}

			file.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			System.exit(-4);
		}
	}

	/**
	 * This method compile the jasmin code into byte code.
	 * 
	 * @param fileName
	 *            name of the jasmin file to compile.
	 */
	private void compileToBytecode(String fileName)
	{
		Jasmin.main(new String[] { fileName });

		if (!keepJFile)
		{
			File file = new File(fileName);
			file.delete();
		}
	}

	/**
	 * Displays the message received if VERBOSE flag is set to true.
	 * 
	 * @param msg
	 *            message to display
	 */
	public static void log(String msg)
	{
		if (Yal2jvm.VERBOSE)
			System.out.println(msg);
	}
}