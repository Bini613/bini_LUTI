// Function in Main. Just action (returns nothing). Arguments: msg (String)
// Prints msg to the Console only when parShowRuntimeOutput (Main parameter, boolean) is true.
// Used for runtime monitoring output (e.g. density after each assignment), so it can be switched on and off
// separately from the test output controlled by parShowTestOutput (funTestPrint).
// Warnings that must always show still use traceln directly.
if (parShowRuntimeOutput) traceln(msg);
