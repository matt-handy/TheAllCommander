package c2;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommandsTest {

	@Test
	void testClientCommandRecognition() {
		assertTrue(Commands.isClientCommand(Commands.CLIENT_CMD_CD + " barf"));
		assertTrue(Commands.isClientCommand(Commands.CLIENT_CMD_PWD));
		assertTrue(Commands.isClientCommand(Commands.CLIENT_CMD_GET_EXE));
		
		assertFalse(Commands.isClientCommand("net user"));
		
		assertFalse(Commands.isClientCommand("random nonsense"));
	}

}
