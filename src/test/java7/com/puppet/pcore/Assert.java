package com.puppet.pcore;

public class Assert {
	public interface Executable {
		void execute() throws Throwable;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Throwable> T assertThrows(Class<? extends Throwable> expectedType, Executable executable) {
		try {
			executable.execute();
		}
		catch (Throwable actualException) {
			if (expectedType.isInstance(actualException)) {
				return (T) actualException;
			}
			else {
				String message = String.format("Expected %s to be throw but %s was thrown instead", expectedType.getName(), actualException.getClass().getName());
				throw new AssertionError(message, actualException);
			}
		}
		throw new AssertionError(
				String.format("Expected %s to be thrown, but nothing was thrown.", expectedType.getName()));
	}
}
