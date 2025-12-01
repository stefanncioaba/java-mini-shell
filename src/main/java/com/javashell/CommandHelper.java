package com.javashell;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

//Helper for the Commands
public class CommandHelper {
    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");
    //Gets the env path
    private static final String path = System.getenv("PATH");
    //Splits the path into dirs
    private static String[] pathDirs = path.split(File.pathSeparator);
    //Gets the current directory : To change
    public static String currentDirectory = System.getProperty("user.dir");
    public static final String[] builtinCommands = {"exit", "echo", "type", "pwd", "cd"};
    //Looks for a file in path and returns its path as a String if it exists and is executable
    public static String lookingForCommand(String fileName) {
        String fullPath = "";
        for (String dir : pathDirs) {
            fullPath = dir + File.separator + fileName;
            //Checks if file exists and is executable
            if (Files.exists(Paths.get(fullPath)) && Files.isExecutable(Paths.get(fullPath))) {
                return fullPath;
            }

            //If we are on windows, we need .exe extension
            if (IS_WINDOWS) {
                String fullPathExe = dir + File.separator + fileName + ".exe";
                if (Files.exists(Paths.get(fullPathExe)) && Files.isExecutable(Paths.get(fullPathExe))) {
                    return fullPathExe;
                }
            }
        }
        return null;
    }

    //Reads file and returns string
    public static String readFile(String fileName) {
        //Gets the absolute path
        Path filePath = Paths.get(fileName);
        if (!filePath.isAbsolute()) {
            filePath = resolveToCurrentDir(fileName);
        }
        //Reads the file
        File myFile = filePath.toFile();
        try(Scanner fileReader = new Scanner(myFile)){
            StringBuilder data = new StringBuilder();
            while(fileReader.hasNextLine()) {
                data.append(fileReader.nextLine());
                if (fileReader.hasNextLine()) {
                    data.append(System.lineSeparator());
                }
            }
            return data.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isBuiltin(String command) {
        if(Arrays.asList(builtinCommands).contains(command)) {
            return true;
        }
        return false;
    }

    /*
        Absolute path means the full path ex: C:\\Users\\home, relative path ex: home
        .rezolve combines it like currentDirectory + "/" + filename
        .normalize gets rid off "./" and "../" by using them as it should
    */
    //Returns the absolute path
    public static Path resolveToCurrentDir(String filename) {
        Path p = Paths.get(filename);
        if (!p.isAbsolute()) {
            p = Paths.get(currentDirectory).resolve(p).normalize();
        }
        return p;
    }


}
