package ch.epfl.gameboj;

/**
 * @author Axel Marmet (288862)
 *
 */
public interface Preconditions {

    /**
     * Throws an IllegalArgumentException if the param b is false
     * 
     * @param condition
     *            the condition that will be evaluated
     * @throws IllegalArgumentException
     */
    static public void checkArgument(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException(
                    "A mistake has been made in the arguments");
        }
    }

    /**
     * Checks that the value given is contained in 8 bits and returns it if it
     * is the case, throws an IllegalArgumentException if it is not.
     * 
     * @param value
     *            the value that will be evaluated (must fit in 8 bits)
     * @throws IllegalArgumentException
     *             if the given value does not fit in 8 bits
     * @return the value passed in the parameters
     */
    static public int checkBits8(int value) {
        checkArgument(0 <= value && value <= 0xFF);
        return value;
    }

    /**
     * Checks that the value given is contained in 16 bits and returns it if it
     * the case, throws an IllegalArgumentException if it is not.
     * 
     * @param value
     *            the value that will be evaluated (must fit in 16 bits)
     * @throws IllegalArgumentException
     *             if the given value does not fit in 8 bits
     * @return the value passed in the parameters
     */
    static public int checkBits16(int value) {
        checkArgument(0 <= value && value <= 0xFFFF);
        return value;
    }

    /**
     * Checks that the given value is strictly positive and can be divided by 32
     * 
     * @param value
     *            the value that will be checked
     * @throws IllegalArgumentException
     *             if the value is not strictly positive or can not be divided
     *             by 32
     */
    static public int checkPositiveAndDivisibleBy32(int value) {
        checkArgument(value > 0 && value % 32 == 0);
        return value;
    }
}
