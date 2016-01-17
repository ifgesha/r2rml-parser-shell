@echo off
echo This is R2RML Parser 0.7-alpha. Run with -h for help on options.
java -Xms128m -Xmx1024m -cp "./*;./lib/*;" gr.seab.r2rml.beans.MainParser %1 %2
echo R2RML Parser 0.7-alpha. Done.
