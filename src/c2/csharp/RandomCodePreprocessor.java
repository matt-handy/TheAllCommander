package c2.csharp;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RandomCodePreprocessor {

	private static boolean isLineAMethodSignature(String line) {
		return line.trim().endsWith(")") && !line.trim().startsWith("if") && !line.trim().startsWith("for") && !line.trim().startsWith("while");
	}
	
	//File processing assumptions:
	//1) each try blocks starts fresh on its own line
	//2) If, while, and for statements each have scope brackets
	public static String processFile(String inputCode) {
		String delineator;
		if(inputCode.contains("\r\n")) {
			delineator = "\r\n";
		}else {
			delineator = "\n";
		}
		
		String lines[] = inputCode.split(delineator);
		StringBuilder output = new StringBuilder();
		
		RandomCodeGenerator generator = new RandomCodeGenerator();
		Stack<VariableScope> scopeStack = new Stack<>();
		
		boolean seenClassDefinition = false;
		boolean seenMethodSignature = false;
		boolean inClass = false;
		boolean inMethod = false;
		boolean seenReturnBeforeScopeChange = false;
		
		boolean tryBlockStarted = false;
		boolean holdUntilCatchStarted = false;
		
		boolean seenScopeBoundaryIdentifierWithoutScopeBracket = false;
		for(String line: lines) {
			//Check if we've entered/exited a class or method, update flags accordingly
			if(!inClass) {
				if(line.contains("class ")) {
					seenClassDefinition = true;
				}
				if(line.contains("{") && !inMethod && seenClassDefinition) {
					inClass = true;
				}
			}else if(inClass && !inMethod) {
				if(isLineAMethodSignature(line) && !inMethod) {
					seenMethodSignature = true;
				}
				
				if(line.contains("{") && !inMethod && seenMethodSignature) {
					inMethod = true;
					scopeStack.clear();
					scopeStack.add(new VariableScope());
				}
				
				if(line.contains("}")) {//Out of a class
					inClass = false;
					seenClassDefinition = false;
				}
			}else if(inClass && inMethod) {
				if(isLineScopeBoundary(line)) {
					seenScopeBoundaryIdentifierWithoutScopeBracket = true;
				}else if(seenScopeBoundaryIdentifierWithoutScopeBracket) {
					if(line.contains("{")) {
						seenScopeBoundaryIdentifierWithoutScopeBracket = false;
					}
				}
				
				if(line.trim().startsWith("return ")) {
					seenReturnBeforeScopeChange = true;
				}else if(startsTryBlock(line)) {
					tryBlockStarted = true;
				}else if(holdUntilCatchStarted && startsCatchBlock(line)) {
					//What happens if the catch statement is a line above the start of scope? No worries, this case is caught
					//with the scope boundary test above.
					holdUntilCatchStarted = false;
				}
				
				long increment = line.chars().filter(ch -> ch == '{').count();
				//inMethodScopeCounters += increment;
				for(int idx = 0; idx < increment; idx++) {
					scopeStack.push(new VariableScope(scopeStack.peek()));
				}
				long decrement = line.chars().filter(ch -> ch == '}').count();
				//inMethodScopeCounters -= decrement;
				for(int idx = 0; idx < decrement; idx++) {
					scopeStack.pop();
					if(scopeStack.empty()) {
						//This is fine, we've reached the end of the method
						inMethod = false;
						seenMethodSignature = false;
						break;
					}
					if(seenReturnBeforeScopeChange) {
						seenReturnBeforeScopeChange = false;
					}
					if(tryBlockStarted) {
						tryBlockStarted = false;
						holdUntilCatchStarted = true;
					}
					
				}
			}
			
			output.append(line + delineator);
			if(!seenScopeBoundaryIdentifierWithoutScopeBracket && !seenReturnBeforeScopeChange && !holdUntilCatchStarted) {
			if(inClass && !inMethod && !seenMethodSignature) {
				String randomMethod = generator.addRandomMethod();
				output.append(randomMethod + delineator);
			}else if(inMethod) {
				output.append(generator.generateNextLineStatement(scopeStack.peek()) + System.lineSeparator());
			}
			}
		}
		
		return output.toString();
	}
	
	private static boolean startsTryBlock(String line) {
		String trimmed = line.trim();
		Pattern tryFinder = Pattern.compile("try\\s*\\{*");
		Matcher matcher = tryFinder.matcher(trimmed);
		if((trimmed.startsWith("try") && matcher.find())) {
			return true;
		}else {
			return false;
		}
	}
	
	private static boolean startsCatchBlock(String line) {
		String trimmed = line.trim();
		Pattern catchFinderIntegratedScope = Pattern.compile("}\\s*catch\\s*\\(");
		Pattern catchFinderStartLine = Pattern.compile("catch\\s*\\(");
		Matcher matcherCatchFinderIntegratedScope = catchFinderIntegratedScope.matcher(trimmed);
		Matcher matcherCatchFinderStartLine = catchFinderStartLine.matcher(trimmed);
		if((trimmed.startsWith("catch") && matcherCatchFinderStartLine.find()) ||
				(trimmed.startsWith("}") && matcherCatchFinderIntegratedScope.find())) {
			return true;
		}else {
			return false;
		}
	}
	
	//This method should return true if an if, for, while, or try statement is made without the corresponding enclosing bracket start.
	private static boolean isLineScopeBoundary(String line) {
		String trimmed = line.trim();
		if(!trimmed.contains("{") && ((trimmed.startsWith("if") || trimmed.startsWith("for") || trimmed.startsWith("while")) && trimmed.endsWith(")")) || trimmed.equalsIgnoreCase("try") || startsCatchBlock(trimmed)) {
			return true;
		}else {
			return false;
		}
	}
}
