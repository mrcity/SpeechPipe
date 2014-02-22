package us.one_dollar.SpeechPipeScribe;

public class DirectDrive extends Object {
	
	/**
	 * Send one single message to the LEDgoes board.  Will be displayed as soon as the board is done displaying current messages.
	 * @param message The text you wish to propagate to the matrix
	 */
	public static void write(String message) {
		
	}

	/**
	 * Send one single message to the LEDgoes board.  Will be displayed as soon as the board is done displaying current messages.
	 * @param message The text you wish to propagate to the matrix
	 */
	public static void writeSingle(String message) {
        MyApp.mLogService.write(message.getBytes());
	}	
}
