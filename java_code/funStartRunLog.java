// Function in Main. Just action (returns nothing). No arguments. Call as the FIRST line of Main's On startup.
// Copies everything printed to the Console (traceln, funTestPrint, funRuntimePrint) into a new text file for this run,
// runs/run_<date>_<time>.txt inside the model folder, while still printing to the Console as normal.
// Writes all of Main's parameter values at the top of the file first.
// Needs two Main variables (type Other): varConsoleOut (java.io.PrintStream) and varLogFile (java.io.PrintStream).
// funStopRunLog (in Main's On destroy) puts the Console back and closes the file.
try {
    java.io.File dir = new java.io.File("runs");
    dir.mkdirs();   // create the runs folder if it doesn't exist yet
    String stamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new java.util.Date());
    java.io.File f = new java.io.File(dir, "run_" + stamp + ".txt");
    varLogFile = new java.io.PrintStream(new java.io.FileOutputStream(f), true);
    varConsoleOut = System.out;   // the Console's own stream, kept so it can be restored at the end

    // Parameters first, so the file records the settings that produced the output below
    varLogFile.println("Run log " + stamp);
    varLogFile.println("Parameters:");
    for (String name : getParameterNames()) varLogFile.println("  " + name + " = " + getParameter(name));
    varLogFile.println();

    // From now on, every printed line goes to both the Console and the file
    java.io.PrintStream console = varConsoleOut;
    java.io.PrintStream file = varLogFile;
    System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
        public void write(int b) { console.write(b); file.write(b); }
        public void write(byte[] b, int off, int len) { console.write(b, off, len); file.write(b, off, len); }
        public void flush() { console.flush(); file.flush(); }
    }, true));
    traceln("Logging this run to " + f.getAbsolutePath());
} catch (Exception e) {
    traceln("Could not start run log: " + e.getMessage());
}
