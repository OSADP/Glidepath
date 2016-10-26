package com.leidos.glidepath.logger;
import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by rushk1 on 10/21/2014.
 */
public class LoggerTester {

    @Test
    public void testCreateLogger() {
        ILogger log = LoggerManager.getLogger(this.getClass());
        assertTrue(log != null);
    }

    @Test
    public void testAddLog1() {
        LoggerManager.setRecordData(true);
        ILogger log = LoggerManager.getLogger(this.getClass());
        LogEntry entry = log.info("test1", "test2");
        assertTrue(entry.getTag().equals("test1"));
        assertTrue(entry.getMessage().equals("test2"));
    }

    @Test
    public void testAddLog2() {
        LoggerManager.setRecordData(true);
        ILogger log = LoggerManager.getLogger(this.getClass());
        LogEntry entry = log.info("test1", "test2");
        assertTrue(entry.getOrigin().equals(this.getClass()));
    }

    @Test
    public void testLogOutputFormat() {
        ILogger log = LoggerManager.getLogger(this.getClass());
        LogEntry entry = log.info("test", "test1");
        LogEntry entry2 = log.output("test11111", "test1");
        LogEntry entry3 = log.debug("testwqewe", "test1");
        LogEntry entry4 = log.error("testfjkal;sdfjk", "test1");
        LogEntry entry5 = log.warn("t", "test1");
        System.out.println(entry);
        System.out.println(entry2);
        System.out.println(entry2);
        System.out.println(entry3);
        System.out.println(entry4);
        System.out.println(entry5);

        assertTrue(entry.toString().length() == entry2.toString().length() &&
                   entry2.toString().length() == entry3.toString().length() &&
                   entry3.toString().length() == entry4.toString().length() &&
                   entry4.toString().length() == entry5.toString().length());
    }
    
    @Test
    public void testFile() {
        LoggerManager.setOutputFile("logs/LoggerTester.txt");
        ILogger log = LoggerManager.getLogger(this.getClass());
        log.info("TAG", "Some message goes here.");
    	try {
			LoggerManager.writeToDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    // Not yet supported, registerLoggerCallback() still throws UnsupportedOperationException
/*    @Test
    public void testLogListener() {
        ILogger log = LoggerManager.getLogger(this.getClass());
        LoggerManager.registerGlobalLoggerCallback(new LogListener() {
            @Override
            public void newLogEntryCallback(LogEntry in) {
                assertTrue(in.getTag().equals("test1"));
                assertTrue(in.getMessage().equals("test2"));
            }
        });

        log.info("test1", "test2");
    }*/
}
