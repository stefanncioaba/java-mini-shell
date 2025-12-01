package com.javashell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private String commandPart;
    private String filePart;

    public Parser(String original) {
        String cleanedOriginal = cleanCommand(original);
        parsedCommand(cleanedOriginal);
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

        if(file.length() > 0){
            commandPart = cleanedCommand;
            filePart = null;
        } else {
            for(int i = 0; i < words.indexOf(token); i++){
                command.append(words.get(i));
                command.append(" ");
            }
            commandPart = command.toString().trim();
            filePart = file.toString().trim();
        }
    }

    public String getCommandPart() {
        return commandPart;
    }

    public String getFilePart() {
        return filePart;
    }
}


