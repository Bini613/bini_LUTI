// TEMPORARY TEST. Function in Main. Just action (returns nothing). No arguments. Called by the cyclic test report event in Main.
// Shows whether any utility term dominates or is negligible, using the sub-utilities each agent stored on its last turn.
// For each agent: share of term i = |U_i| / sum_j |U_j|  (absolute values, since privacy can be positive when VOP < 0).
// Prints the average share of each term over agents, the average size of each term in rand per month,
// and the average expected welfare (logsum, varExpectedWelfare) over the same agents.
// Agents that haven't had a turn yet (all stored sub-utilities still 0) are left out.

// Citizens: rent, travel cost, travel time, privacy
double[] cShare = new double[4];
double[] cSize = new double[4];
double cWelfare = 0;
int nC = 0;
for (Citizen c : citizens) {
    double[] u = {c.varURent, c.varUTravelCost, c.varUTravelTime, c.varUPriv};
    double sumAbs = 0;
    for (double x : u) sumAbs += abs(x);
    if (sumAbs == 0) continue;   // no turn yet
    for (int i = 0; i < 4; i++) {
        cShare[i] += abs(u[i]) / sumAbs;
        cSize[i] += u[i];
    }
    cWelfare += c.varExpectedWelfare;
    nC++;
}
if (nC > 0) {
    String[] names = {"rent", "travel cost", "travel time", "privacy"};
    String s = "Turn " + varTurnCount + " citizen utility shares (" + nC + " citizens):";
    for (int i = 0; i < 4; i++) s += " " + names[i] + " " + roundToDecimal(100 * cShare[i] / nC, 1) + "% (avg " + roundToDecimal(cSize[i] / nC, 0) + ")";
    s += "; avg expected welfare " + roundToDecimal(cWelfare / nC, 0);
    funRuntimePrint(s);   // TEST
}

// Activities: rent, accessibility, location
double[] aShare = new double[3];
double[] aSize = new double[3];
double aWelfare = 0;
int nA = 0;
for (Activity a : activities) {
    double[] u = {a.varURent, a.varUAcc, a.varULoc};
    double sumAbs = abs(u[0]) + abs(u[1]) + abs(u[2]);
    if (sumAbs == 0) continue;   // no turn yet
    for (int i = 0; i < 3; i++) {
        aShare[i] += abs(u[i]) / sumAbs;
        aSize[i] += u[i];
    }
    aWelfare += a.varExpectedWelfare;
    nA++;
}
if (nA > 0) {
    funRuntimePrint("Turn " + varTurnCount + " activity utility shares (" + nA + " activities): rent " + roundToDecimal(100 * aShare[0] / nA, 1)
            + "% (avg " + roundToDecimal(aSize[0] / nA, 0) + "), accessibility " + roundToDecimal(100 * aShare[1] / nA, 1) + "% (avg " + roundToDecimal(aSize[1] / nA, 0)
            + "), location " + roundToDecimal(100 * aShare[2] / nA, 1) + "% (avg " + roundToDecimal(aSize[2] / nA, 0) + "); avg expected welfare " + roundToDecimal(aWelfare / nA, 0));   // TEST
}
