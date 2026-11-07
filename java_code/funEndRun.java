// Function in Main. Just action (returns nothing). Arguments: reason (String)
// Ends the run: prints why it stopped, runs the end-of-run reports, then stops the simulation.
// Called from every stop point (equilibrium reached in testPrintEquilibriumDensity, parMaxRounds reached in funSimulateNextTurn),
// so every run ends with the same reports whichever condition stops it. Add or remove end-of-run reports here.
funRuntimePrint("Run ended at turn " + varTurnCount + " (round " + varRounds + "): " + reason);
testPrintUtilityShares();
finishSimulation();
