package com.javashell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private String original;
    private String commandPart;
    private String filePart;
    private String actualCommand; //Ex : "echo", "cd"
    private String restCommand;

    public Parser(String original) {
        this.original = original;
        String cleanedOriginal = cleanCommand(original);
        parsedCommand(cleanedOriginal);
        commandSplitter(commandPart);
    }


    //Returns the command without quotes and backsalshes
    public String cleanCommand(String original) {
        StringBuilder result = new StringBuilder();
        char c;
        char prevChar;
        for (int i = 0; i < original.length(); i++) {
            c = original.charAt(i);

            //Deals with double quotes
            if (c == '\"') {
                while(++i < original.length() && (c = original.charAt(i)) != '"') {
                    // Handle backslash inside double quotes
                    if (c == '\\' && i + 1 < original.length()) {
                        char next = original.charAt(i + 1);

                        // Only these are escaped inside double quotes
                        if (next == '\\' || next == '"' || next == '$' || next == '`') {
                            result.append(next);
                            i++; // skip the next char
                            continue;
                        }
                    }
                    result.append(c);
                }
                continue;
            }

            //Deals with single quotes
            if (c == '\'') {
                while(++i < original.length() && (c = original.charAt(i)) != '\'') {
                    result.append(c);
                }
                continue;
            }

            //Deals with backslashes outside of quotes
            if (c == '\\') {
                if (i + 1 < original.length()) {
                    result.append(original.charAt(++i));
                }
                continue;
            }

            //Deals with spaces outside quotes
            if(c == ' '){
                if(result.length() > 0 && result.charAt(result.length() - 1) != ' '){
                    result.append(' ');
                    continue;
                }
            }

            result.append(c);
        }
        return result.toString().trim();
    }

    //Splits into command part and file part (looks for 1> || >)
    public void parsedCommand(String cleanedCommand) {
        List<String> words = new ArrayList<String>();
        StringBuilder command = new StringBuilder();
        StringBuilder file = new StringBuilder();
        String token = "";

        //Add every word from the cleaned command
        words.addAll(Arrays.asList(cleanedCommand.split("\\s+")));

        if (words.contains(">")) {
            token = ">";
        } else if (words.contains("1>")) {
            token = "1>";
        }

        if(!token.isEmpty()) {
            for (int i = words.indexOf(token) + 1; i < words.size(); i++) {
                file.append(words.get(i));
                file.append(" ");
            }
        }

        if(file.length() == 0){
            commandPart = cleanedCommand;
            filePart = null;
        } else {
            for(int i = 0; i < cleanedCommand.indexOf(token); i++){
                command.append(cleanedCommand.charAt(i));
            }
            commandPart = command.toString().trim();
            filePart = file.toString().trim();
        }
    }

    //Splits intro actual command (echo, pwd) and the rest
    private void commandSplitter(String commandPart) {
        if(commandPart.contains(" ")) {
            actualCommand = commandPart.substring(0, commandPart.indexOf(" "));
            restCommand = commandPart.substring(commandPart.indexOf(" ") + 1);
        } else {
            actualCommand = commandPart;
        }
    }

    public String getOriginal() {
        return original;
    }

    public String getActualCommand() {
        return actualCommand;
    }

    public String getRestCommand() {
        return restCommand;
    }

    public String getCommandPart() {
        return commandPart;
    }

    public String getFilePart() {
        return filePart;
    }
}


