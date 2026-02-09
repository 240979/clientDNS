/*
    Created by 240979
    This class is created to store logs into file app.log.
    I did not turn off logging into console, because of debugging reasons.
 */
package models;

import lombok.Getter;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.IOException;
import java.util.logging.*;

public class LoggerInitializer {
    protected static Logger LOGGER = Logger.getLogger(LoggerInitializer.class.getName());
    @Getter
    private static TextAreaHandler textAreaHandler;
    public static void init(){
        try{
            Logger rootLogger = Logger.getLogger("");
            FileHandler fileHandler = new FileHandler("warning.log", 5000000, 2, true); // Logging rotation to prevent overgrowing logfiles: 2x 5MB maximum
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.WARNING);
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);
            textAreaHandler = new TextAreaHandler();
            textAreaHandler.setLevel(Level.INFO);
            rootLogger.addHandler(textAreaHandler);

        }catch(IOException e){
            LOGGER.severe(ExceptionUtils.getStackTrace(e)); // If creating logfiles fails, log it at least into stdout
        }
    }
}
