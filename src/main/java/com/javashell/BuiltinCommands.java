package com.javashell;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BuiltinCommands implements ICommand {
    private Parser parser;
    private String filePart;
    private String actualCommand; // ex: echo, type, etc
    private String restCommand;

    public BuiltinCommands(Parser p) {
        this.parser = p;
        this.filePart = parser.getFilePart();
        this.actualCommand = parser.getActualCommand();
        this.restCommand = parser.getRestCommand();
    }

    @Override
    public boolean execute() {
        switch (actualCommand) {
            case "exit":
                return false;

            case "echo":
                echoCommand();
                break;

            case "type":
                typeCommand();
                break;

            case "pwd":
                pwdCommand();
                break;

            case "cd":
                cdCommand();
                break;

            default:
                System.out.println(actualCommand + ": command not found");
        }
        return true;
    }

    private void echoCommand() {
        if (filePart == null) {
            System.out.println(restCommand);
        } else { //For redirection
            try (FileWriter fw = new FileWriter(CommandHelper.resolveToCurrentDir(filePart).toFile())) {
                fw.write(restCommand);
            } catch (IOException e) {
                System.out.println("echo: " + e.getMessage());
            }
        }

    }

    private void typeCommand() {
        //Looks for command to see if it exists and is executable
        String commandPath = CommandHelper.lookingForCommand(restCommand);
        if(CommandHelper.isBuiltin(restCommand)) {
            System.out.println(restCommand + " is a shell builtin");
        } else if(commandPath != null) {
            System.out.println(restCommand + " is " +  commandPath);
        } else {
            System.out.println(restCommand + ": not found");
        }
    }

    private void pwdCommand() {
        System.out.println(CommandHelper.currentDirectory);
    }

    private void cdCommand() {
        Path searchedDir = Paths.get(restCommand);
        String homeDir = System.getenv("HOME");

        if(searchedDir.toString().equals("~")) {
            CommandHelper.currentDirectory = homeDir;
        } else{
            if (!searchedDir.isAbsolute()) {
                searchedDir = CommandHelper.resolveToCurrentDir(searchedDir.toString());
            }

            if(Files.isDirectory(searchedDir)){
                CommandHelper.currentDirectory = searchedDir.toString();
            } else {
                System.out.println("cd: " + restCommand + ": No such file or directory");
            }
        }
    }

}


