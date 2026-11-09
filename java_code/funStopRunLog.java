// Function in Main. Just action (returns nothing). No arguments. Call as the LAST line of Main's On destroy.
// Puts the Console's own output stream back (so the next run doesn't stack a second copy onto this one) and closes this run's log file.
if (varConsoleOut != null) System.setOut(varConsoleOut);
if (varLogFile != null) varLogFile.close();
varConsoleOut = null;
varLogFile = null;
