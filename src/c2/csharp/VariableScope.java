package c2.csharp;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VariableScope {
	private List<String> stringsInScope;
	private List<String> intsInPlay;

	private Random rnd = new SecureRandom();

	public VariableScope() {
		stringsInScope = new ArrayList<>();
		intsInPlay = new ArrayList<>();
	}
	
	public VariableScope(VariableScope parentScope) {
		stringsInScope = new ArrayList<>();
		stringsInScope.addAll(parentScope.stringsInScope);

		intsInPlay = new ArrayList<>();
		intsInPlay.addAll(parentScope.intsInPlay);
	}

	public boolean hasIntegerVariable() {
		return !intsInPlay.isEmpty();
	}
	
	public boolean hasStringVariable() {
		return !stringsInScope.isEmpty();
	}

	public void addIntegerVariable(String variable) {
		intsInPlay.add(variable);
	}
	
	public void addStringVariable(String variable) {
		stringsInScope.add(variable);
	}
	
	public String selectRandomIntVariable() {
		int strVarIndex = rnd.nextInt(intsInPlay.size());
		return intsInPlay.get(strVarIndex);
	}

	public String selectRandomStringVariable(String defaultOption) {
		if (!stringsInScope.isEmpty()) {
			int strVarIndex = rnd.nextInt(stringsInScope.size());
			return stringsInScope.get(strVarIndex);
		} else {
			return defaultOption;
		}
	}
}
