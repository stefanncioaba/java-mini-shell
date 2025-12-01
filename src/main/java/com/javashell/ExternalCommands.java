package com.javashell;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class ExternalCommands implements ICommand {
    Parser parser;
    private String commandPart; //Full command including arguments
    private String filePart;
    private String actualCommand; // ex: echo, type, etc
    private String restCommand;

    public ExternalCommands(Parser p) {
        parser = p;
        this.commandPart = parser.getCommandPart();
        this.filePart = parser.getFilePart();
        this.actualCommand = parser.getActualCommand();
        this.restCommand = parser.getRestCommand();
    }

    //Used to run non-builtins
    @Override
    public boolean execute() {
        if(actualCommand.equals("cat")) {
            catCommand();
            return true;
        }
        String commandPath = CommandHelper.lookingForCommand(actualCommand);

        if(commandPath != null){
            try{
                String[] parts = commandPart.split("\\s+");
                //Creates a process, and runs it
                ProcessBuilder pb = new ProcessBuilder(parts); //Take parts to also see the arguments
                pb.directory(new File(CommandHelper.currentDirectory)); // Sets the process working directory
                pb.inheritIO().start().waitFor();
            }
            catch (Exception e){}
        } else {
            System.out.println(actualCommand + ": command not found");
        }

        return true;
    }

    private void catCommand() {
        StringBuilder output = new StringBuilder();
        String original = parser.getOriginal().trim();
        int firstSpace = original.indexOf(' ');
        String argsPart = original.substring(firstSpace + 1).trim(); //Files that contain spaces in name
        List<String> files = splitForCat(argsPart);
        for (String f : files) {
            if (f.isEmpty()) continue;
            String content = CommandHelper.readFile(f);
            if (content != null) {
                output.append(content);
            } else {
                System.out.println("cat: " + f + ": No such file or directory");
            }
        }
        //For redirection
        if(filePart != null) {
            Path filePath = CommandHelper.resolveToCurrentDir(filePart);
            File myFile  = new File(filePath.toString());
            //Writes it into a file
            try (FileWriter fw = new FileWriter(myFile)) {
                fw.write(output.toString());
            } catch (IOException e) {}
        } else {
            System.out.println(output.toString());
        }
    }

    //Splits ex: '/tmp/cow/f   6' '/tmp/cow/f   59' to "/tmp/cow/f   6", "/tmp/cow/f   59"
    public static List<String> splitForCat(String original) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);

            //Deals with backslash
            if (c == '\\') {
                if (inDouble && i + 1 < original.length()) {
                    char next = original.charAt(i + 1);
                    // Same rules as in cleanCommand for double quotes
                    if (next == '\\' || next == '"' || next == '$' || next == '`') {
                        current.append(next);
                        i++;        // skip the next char
                        continue;
                    }
                }
            }

            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }

            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }

            if (c == ' ' && !inSingle && !inDouble) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }


}
