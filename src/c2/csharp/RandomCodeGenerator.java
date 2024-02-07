package c2.csharp;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomCodeGenerator {

	private enum CodePermutation {INTEGER_MATH, STRING_DEFINITION, WHILE_LOOP, FOR_LOOP, OBJECT_CONSTRUCTOR, TRIVIAL_SLEEP, TRY_CATCH, IF_STATEMENT, INTERNAL_VOID};
	private enum ObjectType {HTTP_CONTENT};
	private enum RandomMethods {INTEGER("int"), STRING("String"), VOID("void"), BOOLEAN("bool");
		public final String codeId;
		private RandomMethods(String codeId) {
			this.codeId = codeId;
		}
	};
	private enum MathOperations {PLUS("+"), MINUS("-"), MULTI("*"), DIV("/"), MOD("%");
		private static Random rnd = new SecureRandom();
		public final String operand;
		private MathOperations(String operand) {
			this.operand = operand;
		}
		public static MathOperations getRandomOperation() {
			return values()[rnd.nextInt(values().length)];
		}
	};
	
	private Random rnd = new SecureRandom();
	private List<String> integerMethodNames = new ArrayList<>();
	private List<String> stringMethodNames = new ArrayList<>();
	private List<String> booleanMethodNames = new ArrayList<>();
	private List<String> voidMethodNames = new ArrayList<>();
	private List<String> generatedStrings = new ArrayList<>();
	
	public String addRandomMethod() {
		int nextMethod = rnd.nextInt(RandomMethods.values().length);
		RandomMethods method = RandomMethods.values()[nextMethod];
		String methodName = generateRandomString();
		
		StringBuilder sb = new StringBuilder();
		VariableScope scope = new VariableScope();
		sb.append("private static " + method.codeId + " " + methodName + "(){" + System.lineSeparator());
		sb.append(generateNextLineStatement(scope) + System.lineSeparator());
		if(method == RandomMethods.INTEGER) {
			sb.append("return " + generateRandomMathPredicate(scope)+ ";" + System.lineSeparator());
		}else if(method == RandomMethods.BOOLEAN) {
			sb.append("return " + buildRandomLogicGate(scope)+ ";" + System.lineSeparator());
		}else if(method == RandomMethods.STRING) {
			sb.append("return " + generateStringAssignmentPredicate(scope)+ ";" + System.lineSeparator());
		}
		sb.append("}");
		
		if(method == RandomMethods.BOOLEAN) {
			booleanMethodNames.add(methodName);
		}else if(method == RandomMethods.INTEGER) {
			integerMethodNames.add(methodName);
		}else if(method == RandomMethods.STRING) {
			stringMethodNames.add(methodName);
		}else {//void
			voidMethodNames.add(methodName);
		}
		
		return sb.toString();
	}
	
	private String getRandomReferenceToRandomBooleanMethod() {
		if(booleanMethodNames.isEmpty()) {
			return rnd.nextInt(10000000) + buildRandomIntLogicOperand() + rnd.nextInt(10000000);
		}else {
			int rndIdx = rnd.nextInt(booleanMethodNames.size());
			return booleanMethodNames.get(rndIdx) + "()";
		}
	}
	
	private String getRandomReferenceToRandomStringMethod() {
		if(stringMethodNames.isEmpty()) {
			return "\"" + generateRandomString() + "\"";
		}else {
			int rndIdx = rnd.nextInt(stringMethodNames.size());
			return stringMethodNames.get(rndIdx) + "()";
		}
	}
	
	private String getRandomReferenceToRandomIntMethod() {
		if(integerMethodNames.isEmpty()) {
			return 1 + rnd.nextInt(100) + "";
		}else {
			int rndIdx = rnd.nextInt(integerMethodNames.size());
			return integerMethodNames.get(rndIdx) + "()";
		}
	}
	
	public String generateNextLineStatement(VariableScope currentScope) {
		int nextOperation = rnd.nextInt(CodePermutation.values().length);
		CodePermutation nextOp =CodePermutation.values()[nextOperation]; 
		if(nextOp == CodePermutation.INTEGER_MATH) {
			return generateRandomMathStatement(currentScope) + ";";
		}else if(nextOp == CodePermutation.OBJECT_CONSTRUCTOR) {
			return generateRandomObject(currentScope) + ";";
		}else if(nextOp == CodePermutation.STRING_DEFINITION) {
			return generateRandomStringVariable(currentScope) + ";";
		}else if(nextOp == CodePermutation.TRIVIAL_SLEEP) {
			return "System.Threading.Thread.Sleep(" + rnd.nextInt(1) +");";
		}else if(nextOp == CodePermutation.WHILE_LOOP) {
			//return buildWhileLoop(currentScope);
			return "System.Threading.Thread.Sleep(" + rnd.nextInt(1) +");";
		}else if(nextOp == CodePermutation.FOR_LOOP) {
			return "System.Threading.Thread.Sleep(" + rnd.nextInt(1) +");";
			//return buildForLoop(currentScope);
		}else if(nextOp == CodePermutation.TRY_CATCH) {
			return buildTryCatch(currentScope);
		}else if(nextOp == CodePermutation.INTERNAL_VOID) {
			if(voidMethodNames.isEmpty()) {//If we don't have any voids to choose from, fall back to something that always works
				return generateRandomObject(currentScope) + ";";
			}else {
				return buildRandomVoidFunctionName() + ";";
			}
		}else {//if statement
			return buildIfStatement(currentScope);
		}
	}
	
	private String buildRandomVoidFunctionName() {
		int rndIdx = rnd.nextInt(voidMethodNames.size());
		return voidMethodNames.get(rndIdx) + "()";
	}
	
	private String buildRandomIntLogicOperand() {
		int choice = rnd.nextInt(5);
		String operand = "";
		switch(choice) {
		case 0:
			operand = " == ";
		case 1:
			operand = " > ";
		case 2:
			operand = " < ";
		case 3:
			operand = " >= ";
		case 4:
			operand = " <= ";
		}
		return operand;
	}
	
	private String buildRandomLogicGate(VariableScope currentScope) {
		StringBuilder sb = new StringBuilder();
		if(rnd.nextBoolean()) { //integer logic
		
		if(currentScope.hasIntegerVariable()) {
			sb.append(currentScope.selectRandomIntVariable());
		}else {
			sb.append(getRandomReferenceToRandomIntMethod());
		}
		sb.append(buildRandomIntLogicOperand());
		sb.append(rnd.nextInt(10000000));
		}else {//Boolean logic
			sb.append(getRandomReferenceToRandomBooleanMethod());
		}
		return sb.toString();
	}
	
	private String buildIfStatement(VariableScope currentScope) {
		StringBuilder sb = new StringBuilder();
		int numberOfBranches = rnd.nextInt(5);
		sb.append("if(" + buildRandomLogicGate(currentScope) + "){" + System.lineSeparator());
		sb.append(generateNextLineStatement(new VariableScope(currentScope)) + System.lineSeparator());
		sb.append("}" + System.lineSeparator());
		for(int idx = 1; idx < numberOfBranches; idx++) {
			sb.append("else if(" + buildRandomLogicGate(currentScope) + "){" + System.lineSeparator());
			sb.append(generateNextLineStatement(new VariableScope(currentScope)) + System.lineSeparator());
			sb.append("}" + System.lineSeparator());
		}
		if(rnd.nextBoolean()) {
			sb.append("else {" + System.lineSeparator());
			sb.append(generateNextLineStatement(new VariableScope(currentScope)) + System.lineSeparator());
			sb.append("}" + System.lineSeparator());
		}
		return sb.toString();
	}
	
	private String buildTryCatch(VariableScope currentScope) {
		StringBuilder sb = new StringBuilder();
		sb.append("try{" + System.lineSeparator());
		sb.append(generateNextLineStatement(new VariableScope(currentScope)) + System.lineSeparator());
		sb.append("}catch(Exception " + generateRandomString() + "){" + System.lineSeparator());
		sb.append(generateNextLineStatement(new VariableScope(currentScope)) + System.lineSeparator());
		sb.append("}" + System.lineSeparator());
		return sb.toString();
	}
	
	private String buildForLoop(VariableScope currentScope) {
		StringBuilder sb = new StringBuilder();
		String counterName = generateRandomString();
		sb.append("for( int " + counterName + " = 0; "+counterName + " < " + rnd.nextInt(5) + "; " + counterName + "++){" + System.lineSeparator());
		sb.append(generateNextLineStatement(new VariableScope(currentScope)) + System.lineSeparator());
		sb.append("}");
		return sb.toString();
	}
	
	private String buildWhileLoop(VariableScope currentScope) {
		StringBuilder sb = new StringBuilder();
		String counterName = generateRandomString();
		sb.append("int " + counterName + " = " + generateRandomMathPredicate(currentScope) + ";" + System.lineSeparator());
		sb.append("while("+counterName + " < " + rnd.nextInt(5) + "){" + System.lineSeparator());
		sb.append(generateNextLineStatement(new VariableScope(currentScope)) + System.lineSeparator());
		sb.append(counterName + "++;" + System.lineSeparator());
		sb.append("}");
		return sb.toString();
	}
	
	private String generateStringAssignmentPredicate(VariableScope currentScope) {
		StringBuilder sb = new StringBuilder();
		int numberOfOperations = 1 + rnd.nextInt(3);
		for(int idx = 0; idx < numberOfOperations; idx++) {
			if(rnd.nextBoolean() && currentScope.hasStringVariable()) {
				sb.append(currentScope.selectRandomStringVariable(generateRandomString()));
			}else {
				if(rnd.nextBoolean()) {
					sb.append(getRandomReferenceToRandomStringMethod());
				}else {
					sb.append("\"" + generateRandomString() + "\"");
				}
				
				
			}
			
			if(idx == numberOfOperations - 1) {
				sb.append(";");
			}else {
				sb.append(" + ");
			}
		}
		return sb.toString();
	}
	
	private String generateRandomStringVariable(VariableScope currentScope) {
		String newVariableName = generateRandomString();
		StringBuilder sb = new StringBuilder();
		sb.append("String ");
		sb.append(newVariableName);
		sb.append(" = ");
		sb.append(generateStringAssignmentPredicate(currentScope));
		currentScope.addStringVariable(newVariableName);
		return sb.toString();
	}
	
	private String generateRandomMathPredicate(VariableScope currentScope) {
		StringBuilder sb = new StringBuilder();
		int numberOfOperations = 1 +rnd.nextInt(3);
		for(int idx = 0; idx < numberOfOperations; idx++) {
			if(rnd.nextBoolean() || !currentScope.hasIntegerVariable()) {
				sb.append(1 + rnd.nextInt(100)); //Make sure we don't give a zero, division is a problem
			}else {
				if(rnd.nextBoolean()) {
					sb.append(getRandomReferenceToRandomIntMethod());
				}else {
					sb.append(currentScope.selectRandomIntVariable());
				}
			}
			
			if(idx == numberOfOperations - 1) {
				sb.append(";");
			}else {
				sb.append(" ");
				sb.append(MathOperations.getRandomOperation().operand);
				sb.append(" ");
			}
		}
		return sb.toString();
	}
	
	private String generateRandomMathStatement(VariableScope currentScope) {
		StringBuilder sb = new StringBuilder();
		String newVariableName = generateRandomString();
		sb.append("int ");
		sb.append(newVariableName);
		sb.append(" = ");
		sb.append(generateRandomMathPredicate(currentScope));
		currentScope.addIntegerVariable(newVariableName);
		return sb.toString();
	}
	
	private String generateRandomString() {
	    int leftLimit = 97; // letter 'a'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 3 + rnd.nextInt(10);

	    String newString = rnd.ints(leftLimit, rightLimit + 1)
	  	      .limit(targetStringLength)
		      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
		      .toString(); 
	    
	    while(generatedStrings.contains(newString)) {
	    	newString = rnd.ints(leftLimit, rightLimit + 1)
	  	  	      .limit(targetStringLength)
	  		      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
	  		      .toString(); 
	    }
	    
	    generatedStrings.add(newString);
	    
	    return newString;

	}
	
	private String generateRandomObject(VariableScope scope) {
		int nextObjectIdx = rnd.nextInt(ObjectType.values().length);
		ObjectType object = ObjectType.values()[nextObjectIdx];
		//if(object == OBJECT_TYPE.HTTP_CONTENT) {
			String payload = scope.selectRandomStringVariable("\"a\"");
			return "HttpContent " + generateRandomString() + " = new StringContent(" + payload + ", Encoding.ASCII, \"text/plain\");";
		//}else {
			//TODO:
		//}
	}
}
