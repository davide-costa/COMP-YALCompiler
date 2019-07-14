**PROJECT TITLE: Yal to JVM compiler


**GROUP: g52
NAME1: Bruno Miguel de Sousa Pinto, NR1: 201502960, GRADE1: 20, CONTRIBUTION1: 25%
NAME2: Davide Henrique Fernandes da Costa, NR2: 201503995, GRADE2: 20, CONTRIBUTION2: 25%
NAME3: Diogo Afonso Duarte Reis, NR3: 201505472, GRADE3: 20, CONTRIBUTION3: 25%
NAME4: Tiago Lascasas dos Santos, NR4: 201503616, GRADE4: 20, CONTRIBUTION4: 25%

GLOBAL Grade of the project: 20


** SUMMARY: 
The developed product is a compiler for the Yal language and whose target is the Java Virtual Machine. The compiler is able to compile any module of this language, supporting ias it was presented in the documentation given. Furthermore, it has some optimizations, with constant propagation, constant folding and register allocation being the most relevant ones.


** EXECUTE: 

java -jar yal2jvm [-r=<0..255>] [-o] [-S] [-v] [-help] <input_file.yal>

-r=<0..255>		number of JVM local vars per function 					(default 255)		(optional)
-o				run three additional code optimizations                      				(optional)
-S				keep the intermediate Jasmin file 					(.j) on the CWD            (optional)
-v				allow verbose output of all compilation stages               (optional)
-help				prints this help and ignores all other options			(optional)
<input_file>.yal		path to the .yal file to compile.                                    				(mandatory)


**DEALING WITH SYNTACTIC ERRORS:
    Our compiler shows information about the lines and columns in which the syntactic errors occurred. It tries to recover from errors, ignoring the characters until a semicolon (;) or a closing bracket (}), depending on the case. This error reporting was implemented by catching and processing the exceptions thrown by the syntactic analyzer.
    When errors are detected, the syntactic analysis is stopped and the errors are shown on the terminal. We stop trying to find and output errors when we reach a maximum of 10 in order to not overload the user with error messages, mainly because many of these errors might be false-positives created by the propagation of previous syntactic errors.



**SEMANTIC ANALYSIS:
 The goal of the semantic analysis in our tool is to verify if the files it analises follow the rules of the Yal language. Our tool handles errors like non-initialization of variables and incompatible types of return in functions. Method returns need to be previously initialized, operations can only be done between variables, constants or array accesses. Also, a variable can’t be equal to the return value of a function if that function’s return type is different from the type of the variable. It checks if in a call the method which is being called in fact exists and if the number of arguments and their types match what is expected.


**INTERMEDIATE REPRESENTATIONS (IRs): 
    We use a single Intermediate Representation, an HLIR. Given the relatively low complexity of the Yal language, we felt that there was no need to come up with both an HLIR and an LLIR. Therefore, our structure serves both functions: it allows for dataflow analysis and register allocation to be performed, it allows for instruction selection and it allows code optimizations, such as the aforementioned constant propagation, folding and efficient While templates.
    The HLIR is a tree structure build from the AST. Each node has its own class depending on its type, but they all extend a generic node called IRNode. The root node is always a node representing the module. Then, the direct descendants are either nodes representing globals or methods, and they have the same order as they had in the source code. Globals don’t have descendants, but methods do. Each method node has, more or less, one direct descendant per source code line. These third-level descendants represent the guts of the source code: variable declarations and initializations, arithmetic expressions, function calls, assignments, and comparisons. The flow control structures, such as the If-else blocks and the While loops, were abstracted through a set of comparison, jump and label nodes. This conversion is made immediately during the construction of the HLIR from the AST. Finally, some fourth-level nodes may exist, such as nodes representing the operands of a comparison or arithmetic expression or nodes representing constants. Needless to say, each of these nodes holds information regarding its functionality: variable names and types, type of operation, method names, constant values, etc.
    All of this code is present in the yal2jvm.hlir package.


**CODE GENERATION: 
    Our compiler generates the JVM instructions using the information of the HLIR. For register allocation we use the HLIR to do the liveness analysis, building the interference graph and then coloring it, giving the registers for the variables. In the end the variables have their registers set. Before the instruction selection can properly begin, however, in case the user activated the optimizations flag, the optimizations regarding constant folding and propagation will be done first (the efficient While template optimization is always done regardless of the flag).
The instructions are selected by doing a depth-first search through the HLIR. Each HLIR node knows how to generate its own instructions. Some nodes may have more than one set of instructions possible, and as such it is chosen the instructions that best fit the contents of that node. A node may also take a peek over its children in order to choose instructions with better coverage or better efficiency. For example, a comparison may take a look at its children (the two operands of the comparison), and if one of them happens to be the constant 0, it then chooses the special instructions for comparison with zero rather than the default comparison instructions. Other example is the use of iinc for a sum operation, in which the operands are verified in order to find out if one of them is a constant. We can, therefore, compare our instruction selection to the one performed by the greedy algorithm Maximal Munch. The lack of instruction variety on the JVM makes instruction selection to have very few possible choices for a given code sample, and the simplicity of the Yal language reduces the available instructions even further. Taking these things into consideration, we believe our solution is close to the optimal in most cases.


**OVERVIEW: 
    In order to develop our tool, we made use of the information taught in the theoretical classes as well as the instructions given by practical classes teacher. We followed the checkpoints guidelines, preparing the deliveries with the expected material, and solving the problems present in the teacher’s feedback. 
When it comes to third-party tools and code, our compiler uses Jasmin to generate the .class file from a .j file (file generated by our tool with human-readable JVM instructions). It is embedded in the source code and compiles alongside the rest of the project, and as such there are no problems with dependencies nor external JAR files. This source code is in the packages jas, jasmin and java_cup.
Also, in order to generate the files used for the lexical and syntactical analysis we used JavaCC which, based on the provided grammar rules of the Yal language, generated these files for us, including the AST.
In order to develop the register allocation functionalities, we used the algorithms presented in the theoretical classes’ slides “Liveness Analysis” and “Heuristics Solution for Graph Coloring”.
As mentioned previously, our instruction selection is similar to the one performed by the Maximal Munch algorithm, although the heuristic is slightly different (we don’t necessarily maximize the coverage, but rather the lower-cost instructions for a given set of nodes as told to us by the JVM documentation).
    


**TESTSUITE AND TEST INFRASTRUCTURE:
    During the development of this tool, we used tests to assure that the changes made to the code wouldn’t generate bad behavior with any of the files used to test the tool.
We tested the files provided by the teachers, as well as some files created by us. We tested semantic analysis with no errors and checked that no errors were detected, we tested files with errors and verified the proper signaling of those errors, and finally we verified that the generated .class files ran successfully. The code used to do that verification is on the source code, in the class AutomatedTests. This class uses JUnit to run unit tests whose purpose is to automatically run each and every one of these files, assessing their successful compilation and eventual execution of the generated .class file.
However, this test suite was used only in mid-development and is not fully suited for the final product. For this, we provide scripts that automatically compile the testing examples and, if the compilation is successful, run the generated .class files. They can be found in the testsuite folder, divided in two folders: no_errors for valid examples and with_errors for invalid ones (ps: the scripts are .cmd batch files and thus run only on Windows, which was the OS used primarily during development by all group members).


**TASK DISTRIBUTION:
Bruno Pinto -> lexical, syntactic and semantic analysis, intermediate representations and some code generation, in particular constant folding. 
Davide Costa-> lexical, syntactic and semantic analysis, intermediate representations and some code generation, constant propagation, efficient while template and graph coloring. 
Tiago Santos -> lexical and syntactic analysis, intermediate representations and code generation, dataflow and liveness analysis, some register allocation (assignment of new registers), automated testing
Diogo Reis-> lexical, syntactic and semantic analysis, intermediate representations and some code generation, documentation. 


**PROS: 
It has some optimizations that make the compiled program more efficient: we provide a very efficient template for While loops, constant folding and constant propagation.
It uses the lower cost instructions whenever possible, including iinc to add a constant to a register and the special instructions for comparison with zero.
During the register allocation, if the specified number of registers is too small, it tells the user in which method it failed to allocate and what is the smallest possible number of registers that allow for a successful allocation on that method.
It has the possibility of providing an extensive log of most of the operations it is internally conducting, such as showing the generated AST and HLIR, the content of the sets during the dataflow and liveness analysis and all the assigned registers and interferences used during the register allocation stage.
It has Jasmin embedded on it and thus does not require that external dependency. The JAR file is, therefore, completely portable, and the code is easily compilable using javac or any other Java compiler. The code is also fully documented using Javadoc, with documentation both on the code and on HTML pages on the /doc directory.


**CONS: 
Some of the errors on the syntactic analysis could use more extensive descriptions.
The use of lookaheads superior to 1 on a few instances during the syntactic analysis.