package com.javashell;


import java.util.Scanner;
import java.util.Arrays;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;


public class Main {
    static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    // Gets the env path
    static String path = System.getenv("PATH");
    static String[] pathDirs = path.split(File.pathSeparator);
    static String currentDirectory = System.getProperty("user.dir");
    static final String[] builtinCommands = {"exit", "echo", "type", "pwd", "cd"};

    public static void main(String[] args) throws Exception {
        String firstWord;
        String restWords;
        //The path of the file we will maybe look for
        String filePath;
        Scanner sc = new Scanner(System.in);
        String command;
        boolean isExit = true;
        while (isExit) {
            System.out.print("$ ");
            command = sc.nextLine();

            CommandWithRedirect parsed = parseRedirection(command);
            String commandLine = parsed.commandPart;   // what we really execute
            String redirectFile = parsed.redirectFile; // null if no > / 1>

            firstWord = "";
            restWords = "";
            //Checks if the input has more than one word
            if (commandLine.indexOf(' ') > -1) {
                firstWord = commandLine.substring(0, commandLine.indexOf(' '));
                restWords = commandLine.substring(commandLine.indexOf(' ') + 1);
            } else {
                firstWord = commandLine;
            }

            switch (firstWord) {
                case "exit":
                    isExit = false;
                    break;

                case "echo":
                    String echoResult = echoVerification(restWords);
                    //Writes it into a file if needed
                    if (redirectFile != null) {
                        try (FileWriter fw = new FileWriter(resolveToCurrentDir(redirectFile))) {
                            fw.write(echoResult);
                        } catch (IOException e) {
                            System.out.println("echo: " + e.getMessage());
                        }
                    } else {
                        System.out.println(echoResult);
                    }
                    break;
                case "cat":
                    //If there is a file where I should redirect
                    if (redirectFile != null) {
                        StringBuilder output = new StringBuilder();
                        if (restWords.contains("'") || restWords.contains("\"")) {
                            // Use the same quote-aware logic as the non-redirect case
                            output.append(catVerification(restWords));
                        } else {
                            String[] files = restWords.split("\\s+");
                            for (String f : files) {
                                if (f.isEmpty()) continue;
                                String content = lookingInFile(Paths.get(f));
                                if (content != null) {
                                    output.append(content);
                                } else {
                                    System.out.println("cat: " + f + ": No such file or directory");
                                }
                            }
                        }


                        //Writes it into a file
                        try (FileWriter fw = new FileWriter(resolveToCurrentDir(redirectFile))) {
                            fw.write(output.toString());
                        } catch (IOException e) {
                        }
                        break;
                    }

                    //If there are no redirected files
                    if (restWords.contains("'") || restWords.contains("\"")) {
                        System.out.println(catVerification(restWords));
                        break;
                    } else {
                        String[] files = restWords.trim().split("\\s+");
                        for (String f : files) {
                            if (f.isEmpty()) continue;
                            String content = lookingInFile(Paths.get(f));
                            if (content != null) {
                                System.out.println(content);
                            } else {
                                System.out.println("cat: " + f + ": No such file or directory");
                            }
                        }
                    }

                    break;

                case "type":
                    if (Arrays.asList(builtinCommands).contains(restWords)) {
                        System.out.println(restWords + " is a shell builtin");
                    } else {
                        filePath = lookingForFile(restWords);
                        if (filePath != null) {
                            System.out.println(restWords + " is " + filePath);
                        } else {
                            System.out.println(restWords + ": not found");
                        }
                    }
                    break;

                case "pwd":
                    System.out.println(currentDirectory);
                    break;

                case "cd":
                    Path searchedDir = Paths.get(restWords);
                    String homeDir = System.getenv("HOME");

                    if(searchedDir.toString().equals("~")) {
                        currentDirectory = homeDir;
                        break;
                    }
                    /*
                    Absolute path means the full path ex: C:\\Users\\home, relative path ex: home
                    .rezolve combines it like currentDirectory + "/" + searchedDir
                    .normalize gets rid off "./" and "../" by using them as it should
                     */
                    if (!searchedDir.isAbsolute()) {
                        searchedDir = Paths.get(currentDirectory).resolve(searchedDir).normalize();
                    }

                    if(Files.isDirectory(searchedDir)){
                        currentDirectory = searchedDir.toString();
                    } else {
                        System.out.println("cd: " + restWords + ": No such file or directory");
                    }
                    break;

                default:
                    //Executes a program if it exists
                    if(execFile(commandLine, redirectFile)) {
                    }else {
                        System.out.println(firstWord + ": command not found");
                    }
                    break;
            }
        }
    }

    //Looks for a file and returns its path as a String if it exists and is executable
    static String lookingForFile(String commandName) {
        if (path != null) {
            String fullPath = "";
            for (String dir : pathDirs) {
                fullPath = dir + File.separator + commandName;
                //Checks if file exists and is executable
                if (Files.exists(Paths.get(fullPath)) && Files.isExecutable(Paths.get(fullPath))) {
                    return fullPath;
                }

                //If we are on windows, we need .exe extension
                if (IS_WINDOWS) {
                    String fullPathExe = dir + File.separator + commandName + ".exe";
                    if (Files.exists(Paths.get(fullPathExe)) && Files.isExecutable(Paths.get(fullPathExe))) {
                        return fullPathExe;
                    }
                }
            }
        }
        return null;
    }

    //Could've been made a lot easier
    //Verifies and returns a given string that may contain quotation marks or backslashes correctly for echo
    static String echoVerification(String givenString) {
        StringBuilder currentWord = new StringBuilder();
        ArrayList<String> words = new ArrayList<>();
        int quoteStartIndex = 0;
        String beforeQuotes = null;
        String insideQuotes = null;
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        for (int i = 0; i < givenString.length(); i++) {

            //Dealing with backslahes outside quotes by immediately adding the next char after them in words list
            if (givenString.charAt(i) == '\\' && !inSingleQuotes &&  !inDoubleQuotes) {
                currentWord.append(givenString.charAt(++i));
                words.add(currentWord.toString());
                currentWord = new StringBuilder();
                continue;
            }

            //Dealing with backslashes inside double quotes
            if(givenString.charAt(i) == '\\' && inDoubleQuotes) {
                //Check the next char
                char nextChar = givenString.charAt(++i);
                if(nextChar == '"' || nextChar == '\\') {
                    currentWord.append(nextChar);
                } else {
                    currentWord.append(givenString.charAt(--i));
                }
                continue;
            }
            //Dealing with single quotes
            if (givenString.charAt(i) == '\'' && !inDoubleQuotes) { //Case when a quote appears
                inSingleQuotes = !inSingleQuotes;
                if(inSingleQuotes) { //If quote sequence just started
                    currentWord.append(givenString.charAt(i));
                    quoteStartIndex = currentWord.length() - 1;
                    continue;
                } else { // If quote sequence is closing
                    if(quoteStartIndex != 0) { //If there is a word before quote sequence
                        beforeQuotes = currentWord.toString();
                        beforeQuotes = beforeQuotes.substring(0, quoteStartIndex);
                        beforeQuotes = beforeQuotes.replaceAll("\\s+", " ");
                        words.add(beforeQuotes);
                    }
                    insideQuotes = currentWord.toString();
                    insideQuotes = insideQuotes.substring(quoteStartIndex + 1, currentWord.length());
                    words.add(insideQuotes);
                    currentWord = new StringBuilder();
                    continue;
                }
            }

            //Dealing with double quotes
            if (givenString.charAt(i) == '\"' && !inSingleQuotes) { //Case when a quote appears
                inDoubleQuotes = !inDoubleQuotes;
                if(inDoubleQuotes) { //If quote sequence just started
                    currentWord.append(givenString.charAt(i));
                    quoteStartIndex = currentWord.length() - 1;
                    continue;
                } else { // If quote sequence is closing
                    if(quoteStartIndex != 0) { //If there is a word before quote sequence
                        beforeQuotes = currentWord.toString();
                        beforeQuotes = beforeQuotes.substring(0, quoteStartIndex);
                        beforeQuotes = beforeQuotes.replaceAll("\\s+", " ");
                        words.add(beforeQuotes);
                    }
                    insideQuotes = currentWord.toString();
                    insideQuotes = insideQuotes.substring(quoteStartIndex + 1, currentWord.length());
                    words.add(insideQuotes);
                    currentWord = new StringBuilder();
                    continue;
                }
            }
            currentWord.append(givenString.charAt(i));
        }
        //Dealing with the last part that is not dealt with inside the loop
        String lastPart = currentWord.toString();
        lastPart = lastPart.replaceAll("\\s+", " ");
        words.add(lastPart);
        return String.join("", words);
    }

    //Could've been made a lot easier
    //Verifies a given string that may contain quotation marks or backslashes correctly, read the files and return their content
    static String catVerification(String givenString) {
        StringBuilder currentWord = new StringBuilder();
        ArrayList<String> words = new ArrayList<>();
        String beforeQuotes = null;
        String insideQuotes = null;
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        for (int i = 0; i < givenString.length(); i++) {
            //Dealing with backslashes inside double quotes
            if(givenString.charAt(i) == '\\' && inDoubleQuotes) {
                char nextChar = givenString.charAt(++i);
                if(nextChar == '"' || nextChar == '\\') {
                    currentWord.append(nextChar);
                } else {
                    currentWord.append(givenString.charAt(--i));
                }
                continue;
            }

            //Dealing with single quotes
            if (givenString.charAt(i) == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                //If quote sequence is closing
                if(!inSingleQuotes) {
                    insideQuotes = currentWord.toString().trim();
                    if(lookingInFile(Paths.get(insideQuotes)) != null) {
                        words.add(lookingInFile(Paths.get(insideQuotes)));
                        currentWord = new StringBuilder();
                        continue;
                    }else  {
                        words.add("This file does not exist or can not be opened ");
                        currentWord = new StringBuilder();
                        continue;
                    }
                }
                continue;
            }

            //Dealing with double quotes
            if (givenString.charAt(i) == '\"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                //If quote sequence is closing
                if(!inDoubleQuotes) {
                    insideQuotes = currentWord.toString().trim();
                    if(lookingInFile(Paths.get(insideQuotes)) != null) {
                        words.add(lookingInFile(Paths.get(insideQuotes)));
                        currentWord = new StringBuilder();
                        continue;
                    }else  {
                        words.add("This file does not exist or can not be opened ");
                        currentWord = new StringBuilder();
                        continue;
                    }
                }
                continue;
            }
            //Only add if we are inside quotes
            if(inSingleQuotes || inDoubleQuotes) {
                currentWord.append(givenString.charAt(i));
            }
        }

        return String.join("", words);
    }

    //Executing a file from a string that may contain quotation marks and returning true if the file exists and can be executed
    //Supports running and sending the output to another file
    static boolean execFile(String givenString, String redirectFile) {
        StringBuilder currentWord = new StringBuilder();
        ArrayList<String> words = new ArrayList<>();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        char c;
        for (int i = 0; i < givenString.length(); i++) {
            c =  givenString.charAt(i);

            //Dealing with backslashes inside double quotes
            if(c == '\\' && inDoubleQuotes) {
                //Check the next char
                char nextChar = givenString.charAt(++i);
                if(nextChar == '"' || nextChar == '\\') {
                    currentWord.append(nextChar);
                } else {
                    c = givenString.charAt(--i);
                    currentWord.append(c);
                }
                continue;
            }

            //Dealing with single quotes
            if(c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                while(++i < givenString.length() && (c = givenString.charAt(i)) != '\'') {
                    currentWord.append(c);
                }
                words.add(currentWord.toString());
                currentWord = new StringBuilder();
                continue;
            }

            //Dealing with double quotes
            if(c == '\"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                while(++i < givenString.length() && (c = givenString.charAt(i)) != '\"') {
                    currentWord.append(c);
                }
                words.add(currentWord.toString());
                currentWord = new StringBuilder();
                continue;
            }

            //If there is a space not in the quotation marks, the command is finished and we start looking for arguments
            if(c == ' ' && !inSingleQuotes && !inDoubleQuotes) {
                words.add(currentWord.toString());
                currentWord = new StringBuilder();
                continue;
            }
            currentWord.append(c);
        }
        words.add(currentWord.toString().trim());
        //Looking the file to see if it is executable
        String command = words.get(0);
        words.remove(0);
        if(lookingForFile(command) != null){
            ArrayList<String> fullCommand = new ArrayList<String>();
            //Creating the full command to run
            fullCommand.add(command);
            //Adds the arguments
            for (String w : words) {
                if (!w.isEmpty()) {
                    fullCommand.add(w);
                }
            }
            try{
                //Creates a process, and runs it
                ProcessBuilder pb = new ProcessBuilder(fullCommand);
                pb.directory(new File(currentDirectory)); // Sets the process working directory
                if (redirectFile != null) {
                    redirectFile = String.valueOf(resolveToCurrentDir(redirectFile));
                    pb.redirectOutput(new File(redirectFile)); // stdout -> file
                } else {
                    pb.inheritIO();                           // stdout -> console
                }
                pb.start().waitFor();
            }
            catch (Exception e){
                System.out.println(e.getMessage());
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    //Reads a file and returns what is inside or returns null if it doesn't exist
    static String lookingInFile(Path filePath) {
        // If the path is relative, make it relative to our shell's currentDirectory
        if (!filePath.isAbsolute()) {
            filePath = Paths.get(currentDirectory).resolve(filePath).normalize();
        }
        File myFile = filePath.toFile();
        try(Scanner fileReader = new Scanner(myFile)){
            String data = "";
            while(fileReader.hasNextLine()) {
                data += fileReader.nextLine();
                if (fileReader.hasNextLine()) {
                    data += "\n";
                }
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    //Makes the full string into 2 parts: the command and the file it needs to be redirected to
    static class CommandWithRedirect {
        String commandPart;   // the actual command without redirection
        String redirectFile;  // null if no redirection

        CommandWithRedirect(String commandPart, String redirectFile) {
            this.commandPart = commandPart;
            this.redirectFile = redirectFile;
        }
    }

    //parse the command if it has > or 1> inside it, or returns CommandWithRedirect with null properties
    static CommandWithRedirect parseRedirection(String givenString) {
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        int gtIndex = -1;
        for (int i = 0; i < givenString.length(); i++) {
            char c = givenString.charAt(i);
            if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '>' && !inSingleQuotes && !inDoubleQuotes) {
                gtIndex = i;
                break;
            }
        }
        // No redirection at all
        if (gtIndex == -1) {
            return new CommandWithRedirect(givenString.trim(), null);
        }

        //Getting the command part
        int cmdEnd = gtIndex - 1; // the char before >

        //if we have 1 before >
        if(cmdEnd >= 0 && givenString.charAt(cmdEnd) == '1') {
            cmdEnd--;
        }
        //Get rid of white spaces before 1> or >
        while (cmdEnd >= 0 && Character.isWhitespace(givenString.charAt(cmdEnd))) cmdEnd--;
        String cmdPart = givenString.substring(0, cmdEnd + 1).trim();

        //Getting the file part
        int fileStart = gtIndex + 1;
        while (fileStart < givenString.length() && Character.isWhitespace(givenString.charAt(fileStart))) {
            fileStart++;
        }
        String filename = givenString.substring(gtIndex + 1).trim();
        return new CommandWithRedirect(cmdPart, filename.isEmpty() ? null : filename);
    }

    //Makes the path absolute to the current directory
    static File resolveToCurrentDir(String filename) {
        Path p = Paths.get(filename);
        if (!p.isAbsolute()) {
            p = Paths.get(currentDirectory).resolve(p).normalize();
        }
        return p.toFile();
    }

}

