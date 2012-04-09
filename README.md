DCPU 16 Assembler
=================

This is an assembler for the DCPU-16 architecture as designed by Notch. This
implements version 1.1 of the standard available at
http://0x10c.com/doc/dcpu-16.txt

This assembler also supports the DAT, BRK, and JMP instructions.

The DAT instruction is followed by data to put directly in the compiled
binary. This data is a comma-delimited list of strings, characters, integers,
and address labels.

The BRK instruction signals for the program to end. Currently this compiles to
the same code that "SUB PC, 1" does.

The JMP instruction takes one argument: a label or address to jump to. It is
equivalent to using "SET PC, value", except that it will automatically be
replaced by "ADD PC, delta" or "SUB PC, delta" if it is found that those
instructions would be shorter. If optimizations are enabled (which they are by
default), then all eligible uses of "SET PC, value" in the source file will
also be considered for this optimization.

Input Source Notes
------------------

Integers may be specified as a decimal value, or they may be specified in
hexadecimal by prepending them with "0x" (i.e. "0x1f").

Source files are expected to be in UTF-8 format.

The Unicode code point value of a single character can be used where ever an
integer is expected. Simply put the character in single quotes (i.e. 'a').

Strings may include Unicode characters, and they will be encoded into the
compiled binary as UTF-16. Notch's DCPU-16 implementation in 0x10c may or may
not natively support non-ASCII values.

Obtaining
---------

This software project has compiled releases hosted at
https://github.com/AgentME/DCPU-16-Assembler/downloads

(Make sure you download one of the named .zip files under the
"Download Packages" section! The "Download as zip" and "Download as
tar.gz" buttons just lead you to the project's source code.)

Usage
-----

On most platforms, the jar file can be run from the command line as follows:

    java -jar DCPU16Assembler.jar

Running it with no more arguments like that will cause it to show the usage
instructions. To use it, pass the name of the source file:

    java -jar DCPU16Assembler.jar myprogram.asm

By default, the compiled binary will be written to a file named "a.out". You
can specify an output filename by using a second argument:

    java -jar DCPU16Assembler.jar myprogram.asm myprogram.bin

The -h/--help option can be given to show the usage instructions and list of
options, and the --no-optimizations option can be given to disable all
automatic optimizations while compiling.

This assembler support writing its binaries in both little endian and
big endian format. The assembler defaults to little endian format. The
format may be specified with one of the -l/--little-endian or
-b/--big-endian options.

License
-------

Copyright (C) 2012 Chris Cowan (agentme49@gmail.com)

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program.  If not, see <http://www.gnu.org/licenses/>.
