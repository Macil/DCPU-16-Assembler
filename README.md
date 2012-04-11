DCPU 16 Assembler
=================

This is an assembler for the DCPU-16 architecture as designed by
Notch. This implements version 1.1 of the standard available at
http://0x10c.com/doc/dcpu-16.txt

This assembler also supports the DAT/DATA, BRK, JMP, RESERVE/.DS,
TIMES/DUP, and .INCBIN meta-instructions that work on all DCPU-16
implementations, and it allows the user to specify custom new
non-basic opcode instructions for use with supporting DCPU-16
implementations.

The DAT or DATA instruction is followed by data to put directly in the
compiled binary. This data is a comma-delimited list of strings,
characters, integers, and address labels.

The BRK instruction signals for the program to end. Currently this
compiles to the same code that "SUB PC, 1" does.

The JMP instruction takes one argument: a label or address to jump
to. It is equivalent to using "SET PC, value", except that it will
automatically be replaced by "ADD PC, delta" or "SUB PC, delta" if it
is found that those instructions would be shorter. If optimizations
are enabled (which they are by default), then all eligible uses of
"SET PC, value" in the source file will also be considered for this
optimization.

The RESERVE or .DS instruction takes one integer argument specifying
how many words of zeroes to reserve in memory at its location. `.DS 3`
is equivalent to `DAT 0, 0, 0`.

The TIMES or DUP instruction can be used immediately before a DAT
instruction with a number that tells how many times to repeat the DAT
instruction. `TIMES 3 DAT 5` is equivalent to `DAT 5, 5, 5`.

The .INCBIN instruction is followed by a quoted filename of a file to
include into the compiled binary at this location. The given pathname
must be relative to the source file's path. It is then followed by an
optional endianness specifier, "THIS" (default), "LE", or "BE". "THIS"
specifies that the included file should be read in the same endianness
that this source file is being compiled to, "LE" means it should be
read in little endian form, and "BE" means it should be read in big
endian form. If the file is included in the same endian form that the
source file is being compiled to, then the contents of the file will
exactly match what is written into the final binary. Example usage:
`.incbin "some file.bin" le`

Custom NB-Opcodes
-----------------

The DCPU-16 standard defines a type of opcode ("non-basic opcode", or
"NB-opcode" for short) that takes one argument. Only one NB-opcode is
specified in the standard (JSR, with the value of 1).

Custom DCPU-16 implementations may implement other NB-opcodes, so this
assembler allows the user to specify custom NB-opcodes and their
values on the command line. The value of a custom NB-opcode must be
between 2 and 63. The -n/--new-nbopcode options may be used (multiple
times even) to specify the custom NB-opcodes. Look at the Usage
section for examples.

Input Source Notes
------------------

Integers may be specified as a decimal value, or they may be specified
in hexadecimal by prepending them with "0x" (i.e. "0x1f").

Source files are expected to be in UTF-8 format. If you don't use any
non-7-bit characters, then normal ASCII format works as it is a subset
of UTF-8. Both Unix and DOS style line endings are supported.

The Unicode code point value of a single character can be used where
ever an integer is expected. Simply put the character in single quotes
(i.e. 'a').

Strings may include Unicode characters, and they will be encoded into
the compiled binary as UTF-16. Notch's DCPU-16 implementation in 0x10c
may or may not natively support non-ASCII values.

Obtaining
---------

This software project has compiled releases hosted at
https://github.com/AgentME/DCPU-16-Assembler/downloads

(Make sure you download one of the named .zip files under the
"Download Packages" section! The "Download as zip" and "Download as
tar.gz" buttons just lead you to the project's source code.)

Usage
-----

Java is required to use this assembler. On most platforms, the jar
file can be run from the command line as follows:

    java -jar DCPU16Assembler.jar

On Windows, the java executable may not be in your default search
path. If the above line does not work, then find your Java
installation's directory, and customize the following command to fit
your system. The following command is not permanent and will only
affect this current session:

    set path=%path%;C:\Program Files (x86)\Java\jdk1.6.0_07\bin

If you got that right, then the following command should work now:

    java -jar DCPU16Assembler.jar

Running it with no more arguments like that will cause it to show the
usage instructions. To use it, pass the name of the source file:

    java -jar DCPU16Assembler.jar myprogram.asm

By default, the compiled binary will be written to a file named
"a.out". You can specify an output filename by using a second
argument:

    java -jar DCPU16Assembler.jar myprogram.asm myprogram.bin

The -h/--help option can be given to show the usage instructions and
list of options, and the --no-optimizations option can be given to
disable all automatic optimizations while compiling.

This assembler support writing its binaries in both little endian and
big endian format. The assembler defaults to little endian format. The
format may be specified with one of the -l/--little-endian or
-b/--big-endian options. You may want to check which format(s) your
DCPU-16 implementations support.

Custom NB-opcodes may be specified on the command line as follows with
either the -n or --new-nbopcode arguments:

    java -jar DCPU16Assembler.jar test.asm -n print 17 -n clear 18

When those options are used, then "print" can be used in the source
file just like other NB-opcodes like "jsr". Remember, these custom
NB-opcodes are not meta-instructions that work everywhere; instead
they are unstandard instructions that must be supported by your
DCPU-16 implementation in order to be useful.

License
-------

Copyright (C) 2012 Chris Cowan (agentme49@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
