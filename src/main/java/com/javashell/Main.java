package com.javashell;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        boolean running = true;
        Scanner sc = new Scanner(System.in);
        while (running) {
            System.out.print("$ ");
            String command = sc.nextLine();
            Parser p = new Parser(command);
            if(CommandHelper.isBuiltin(p.getActualCommand())) {
                BuiltinCommands builtinCommands = new BuiltinCommands(p);
                running = builtinCommands.execute();
            } else {
                ExternalCommands externalCommands = new ExternalCommands(p);
                externalCommands.execute();
            }


        }
    }
}