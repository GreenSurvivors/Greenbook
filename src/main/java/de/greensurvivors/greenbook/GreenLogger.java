package de.greensurvivors.greenbook;

import org.bukkit.ChatColor;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GreenLogger {

    private static Logger logger;

    /**
     * Set plugin logger.
     *
     * @param logger to log to
     */
    public static void setLogger(Logger logger) {
        GreenLogger.logger = logger;
    }

    /**
     * Log a message, with no arguments.
     * If the logger is currently enabled for the given message level then the given message is forwarded to all the registered output Handler objects.
     *
     * @param level   One of the message level identifiers, e.g., SEVERE
     * @param message The string message (or a key in the message catalog)
     */
    public static void log(Level level, String message) {
        //the paper logger doesn't support color anymore. But as long as using chatColor still works we will use it
        logger.log(level, ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Log a message, with associated Throwable information.
     * If the logger is currently enabled for the given message level then the given arguments are stored in a LogRecord which is forwarded to all registered output handlers.
     * Note that the thrown argument is stored in the LogRecord thrown property, rather than the LogRecord parameters property. Thus it is processed specially by output Formatters and is not treated as a formatting parameter to the LogRecord message property.
     *
     * @param level   One of the message level identifiers, e.g., SEVERE
     * @param message The string message (or a key in the message catalog)
     * @param thrown  Throwable associated with log message.
     */
    public static void log(Level level, String message, Throwable thrown) {
        //the paper logger doesn't support color anymore. But as long as using chatColor still works we will use it
        //todo ^ this is NOT true
        logger.log(level, ChatColor.translateAlternateColorCodes('&', message), thrown);
    }

}