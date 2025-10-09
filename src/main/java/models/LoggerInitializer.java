/*
    Created by 240979
    This class is created to store logs into file app.log.
    I did not turn off logging into console, because of debugging reasons.
 */
package models;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.logging.*;

public class LoggerInitializer {
    protected static Logger LOGGER = Logger.getLogger(LoggerInitializer.class.getName());
    public static void init(){
        try{
            Logger rootLogger = Logger.getLogger("");
            FileHandler fileHandler = new FileHandler("warning.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.WARNING);

            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);
        }catch(IOException e)
        {
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
        }
    }
}
