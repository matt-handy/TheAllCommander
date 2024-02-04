using System;
using System.Text;

namespace Example
{
    static class HelloWorld
    {
		
		//TODO: This is a total hack to make the stager work, as it only seems to behave with
		//a static member with no arguments
		public static void FakeMain(){
			Main(new string[0]);
		}
		
		public static void Main(string[] args) 
        {
			Console.WriteLine("Hello world");
		}
	}
	
}