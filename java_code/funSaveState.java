// Function in Main. Just action (returns nothing). No arguments. Called by funRunCheckpoint at the first spread equilibrium.
// Saves the settled city to savedState.txt in the model folder (overwritten each time), so later runs can start from it
// with funLoadState instead of waiting for the first equilibrium again. The state belongs to the network in savedNetwork.txt,
// which is always this run's network: either loaded from it, or built and saved to it by funBuildNetwork.
// Saved: every Main parameter (to check the state still matches the settings when loading), then
//   CITIZEN  home node index, VOT, VOM, VOP, expected welfare (logsum from its last turn, used by funCheckWelfarePeak),
//            then one weight per activity
//   ACTIVITY location node index, VOM, VOL          (in population order, so weight positions still match activities)
//   LINK     node A index, node B index, impedance, flow
// Land, density, rent and travel times are not saved: they are rebuilt from these after loading.
// Numbers are written with Java's own formatting (always '.' as decimal point), so they read back exactly.
// Switched off by parSaveState (Main parameter, boolean): set it false in experiments, so runs don't overwrite each other's
// saved state (or clash writing the same file when running in parallel). The run itself continues identically either way.
if (!parSaveState) {
    funRuntimePrint("State not saved (parSaveState is false)");
    return;
}
try (java.io.PrintWriter out = new java.io.PrintWriter("savedState.txt")) {
    out.println("SAVED_STATE turn " + varTurnCount + " round " + varRounds);
    for (String name : getParameterNames()) out.println("PARAM\t" + name + "\t" + getParameter(name));
    for (Citizen c : citizens) {
        StringBuilder s = new StringBuilder("CITIZEN\t" + c.varHome.parIndex + "\t" + c.parVOT + "\t" + c.parVOM + "\t" + c.parVOP + "\t" + c.varExpectedWelfare);
        for (double w : c.varActivityWeights) s.append("\t").append(w);
        out.println(s);
    }
    for (Activity a : activities) out.println("ACTIVITY\t" + a.varLocation.parIndex + "\t" + a.parVOM + "\t" + a.parVOL);
    for (Link l : links) out.println("LINK\t" + l.parNodeA.parIndex + "\t" + l.parNodeB.parIndex + "\t" + l.varImpedance + "\t" + l.varFlowVol);
    funRuntimePrint("State saved to savedState.txt at turn " + varTurnCount + " (round " + varRounds + ")");
} catch (Exception e) {
    traceln("Could not save state: " + e.getMessage());
}
