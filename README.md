# Java Mini Shell

A small Unix-like shell implemented in Java.  
It supports basic built-in commands, external programs, quoting, and simple output redirection – built as a learning project to better understand parsing, processes, and the filesystem.

---

## Features

- **Built-in commands**
  - `exit` – exit the shell
  - `echo` – print text with support for quotes and backslashes
  - `pwd` – print the current working directory
  - `cd` – change directory (supports relative paths and `~` for `$HOME`)
  - `type` – show whether a command is a builtin or the path to an external executable

- **External commands**
  - Resolves commands using the system `PATH`
  - Runs programs in the shell’s current directory

- **`cat` implementation**
  - `cat file1 file2 ...` – print the contents of one or more files
  - Arguments are resolved relative to the shell’s current directory
  - Supports quoted filenames: `cat "some file.txt"`

- **Output redirection**
  - `> file` and `1> file` for redirecting stdout to a file  
    Examples:
    - `echo hello > out.txt`
    - `cat a.txt b.txt 1> all.txt`
  - Redirection is ignored when it appears inside quotes

- **Quoting & escaping**
  - Single quotes `'...'` and double quotes `"..."` handled separately
  - Inside double quotes, backslashes can escape `"` and `\`
  - Special parsing for:
    - `echo` arguments
    - `cat` arguments
    - arbitrary commands passed to the process runner

---

## Limitations / Future Work

- Current implementation is a single-class prototype; next step is to refactor
  into separate components (shell loop, command parser, command executor).
- Parsing logic for quoting/escaping is ad-hoc and focused on the supported
  features; a proper tokenizer + parser would make it easier to extend with
  pipes and more redirection operators.
- Currently supports only `>` / `1>` for stdout redirection; stderr and pipes
  (`|`) are not yet implemented.

---

## Example Session

```text
$ pwd
/home/user/projects/java-mini-shell

$ echo hello world
hello world

$ echo "hello   world"
hello   world

$ cd src
$ pwd
/home/user/projects/java-mini-shell/src

$ echo "output to file" > out.txt
$ cat out.txt
output to file

$ type echo
echo is a shell builtin

$ type java
java is /usr/bin/java

$ exit
