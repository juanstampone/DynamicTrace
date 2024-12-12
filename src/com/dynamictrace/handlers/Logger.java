package com.dynamictrace.handlers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Logger {

	public static final String emptyMethodInfo = "";
    public static final String MethodHeader = 
    		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
			"<trace_Metamodel:ExecutionTrace xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
			"    xmlns:trace_Metamodel=\"http://www.example.org/trace_Metamodel\" xsi:schemaLocation=\"http://www.example.org/trace_Metamodel trace_Metamodel.ecore\">";
	
	private static Logger instance = new Logger();
	private List<String> record = new LinkedList<String>();
    private static int currentLevel = 0;

   
    private Logger() {
    }
    
	public static Logger getLoggingClient() {
		return instance;
	}
	
	
	public void pushLevel(String statement) {
        final int level = currentLevel++;
        this.record.add(statement);
     //   Platform.runLater(() -> controller.startMethod(level, statement));
    }

    public void currentLevel(String statement) {
        final int level = currentLevel;
        this.record.add(statement);
     //   Platform.runLater(() -> controller.startMethod(level, statement));
    }

    public void popLevel(String statement) {
        final int level = --currentLevel;
        this.record.add(statement);
     //   Platform.runLater(() -> controller.endMethod(level, statement));
    }
	
	public void exportLogger(File selectedFile) {	
		try {		
			FileWriter writer = new FileWriter(selectedFile);
			
			for (int count = 0; count < this.record.size(); count++) {
				writer.write((String) this.record.get(count));
				writer.write("\r\n");
			}
			writer.flush();
			
		} catch (IOException e) {
			System.out.println("Error in Logging Client: " + e);
		}
	}
	
	public void cleanLogger() {
		this.record.clear();
	}
}