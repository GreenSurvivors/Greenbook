package de.greensurvivors.greenbook.utils;

/**
 * utils that doesn't belong anywhere else, currently contains a test if a string is an int, used by commands
 */
public class MiscUtil {
    private static final int MAX_INT_CHARS = (int) (Math.log10(Integer.MAX_VALUE) + 1);

    /**
     * Test if a String can safely convert into an integer
     * @param toTest String input
     * @return boolean, is only true if the Sting has the right size and is entirely made of numbers (first char might be a '-')
     */
    public static boolean isInt (String toTest){
        if (toTest == null) {
            return false;
        }

        int length = toTest.length();
        if (length > MAX_INT_CHARS){ //protection against overflow
            return false;
        } else if (length == 0) { //empty
            return false;
        }

        int i = 0;
        if (toTest.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = toTest.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
