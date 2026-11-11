// Function in Main. Returns value, type: boolean. No arguments. Called by Main on startup, in place of funInitImpedance and
// funPlaceAgents, only when the road network was loaded from savedNetwork.txt and parLoadSavedState is true.
// Recreates the city saved by funSaveState (savedState.txt) at its first density equilibrium: citizens and activities
// with their saved locations and preferences, and every link's impedance and flow.
// Returns true if loaded; false if there is no usable file, in which case nothing has been created and the
// caller places agents as normal (funPlaceAgents).
//
// The file is only used if it matches this run:
//   - Every Main parameter must have the same value as when it was saved, except those in 'ignored' below
//     (investment, display, output and run-control settings, which don't affect how the city settled). Edit that list as needed.
//   - Every saved link must exist in the current network, and the link counts must match (same savedNetwork.txt).
// After loading:
//   - Preferences are not drawn for loaded agents (funPlaceAgents does that, not the agents' On startup): the saved values are set directly.
//   - The initialisation phase is skipped (varInitialising = false), the state is not saved again (varStateSaved = true),
//     and the first checkpoint goes straight to investment (varAtLoadedEquilibrium = true), since the city is already settled.
// Main on startup then sets up land and calls funResumeFromEquilibrium (reseed with parSeed + 2, density, traffic, queue,
// counters), the same restart the straight-through run goes through at its first equilibrium, so both continue identically.

java.io.File f = new java.io.File("savedState.txt");
if (!f.exists()) {
    traceln("No saved state found: placing agents as normal");
    return false;
}

String[] ignored = {"parRoadUpgradeCost", "parInvestmentRate", "parShowTestOutput", "parShowRuntimeOutput", "parIconJitter",
                    "parMaxRdPx", "parMinRdPx", "parRentColourScaleMultiple", "parMaxRounds", "parMinRounds",
                    "parLoadSavedNetwork", "parLoadSavedState", "parRelocationInterval", "parParallelAssignment"};

// 1. Read and check the whole file before creating anything, so a bad or outdated file leaves the model untouched
List<String[]> savedCitizens = new ArrayList<>();
List<String[]> savedActivities = new ArrayList<>();
List<String[]> savedLinks = new ArrayList<>();
try {
    List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
    Set<String> ignoredSet = new HashSet<>(Arrays.asList(ignored));
    List<String> mismatches = new ArrayList<>();
    for (String line : lines) {
        String[] v = line.split("\t");
        switch (v[0]) {
            case "PARAM":
                if (ignoredSet.contains(v[1])) break;
                Object nowValue = getParameter(v[1]);   // null if the parameter no longer exists. Stored as Object first: passed straight into
                String now = String.valueOf(nowValue);  // String.valueOf, Java would pick its char[] version and fail on numbers
                String saved = v.length > 2 ? v[2] : "";
                if (!saved.equals(now)) mismatches.add(v[1] + " (saved " + saved + ", now " + now + ")");
                break;
            case "CITIZEN":  savedCitizens.add(v); break;
            case "ACTIVITY": savedActivities.add(v); break;
            case "LINK":     savedLinks.add(v); break;
        }
    }
    if (!mismatches.isEmpty()) {
        traceln("Saved state not used, parameters differ: " + String.join(", ", mismatches));
        return false;
    }
    if (savedLinks.size() != links.size()) {
        traceln("Saved state not used: it has " + savedLinks.size() + " links, the current network has " + links.size());
        return false;
    }
    for (String[] v : savedLinks) {
        int ka = Integer.parseInt(v[1]), kb = Integer.parseInt(v[2]);
        if (funFindLink(varGrid[ka / parCols][ka % parCols], varGrid[kb / parCols][kb % parCols]) == null) {
            traceln("Saved state not used: it belongs to a different road network");
            return false;
        }
    }
} catch (Exception e) {
    traceln("Could not read saved state: " + e.getMessage());
    return false;
}

// 2. Activities first (in saved order, so each citizen's weight positions match activity indices)
for (String[] v : savedActivities) {
    int k = Integer.parseInt(v[1]);
    GridPoint g = varGrid[k / parCols][k % parCols];
    Activity a = add_activities();   // preferences set from the file below
    a.parVOM = Double.parseDouble(v[2]);
    a.parVOL = Double.parseDouble(v[3]);
    a.varLocation = g;
    g.colActivities.add(a);
    funPlaceIcon(a, g);
}

// 3. Citizens
for (String[] v : savedCitizens) {
    int k = Integer.parseInt(v[1]);
    GridPoint g = varGrid[k / parCols][k % parCols];
    Citizen c = add_citizens();   // preferences set from the file below
    c.parVOT = Double.parseDouble(v[2]);
    c.parVOM = Double.parseDouble(v[3]);
    c.parVOP = Double.parseDouble(v[4]);
    c.varActivityWeights = new double[v.length - 5];
    for (int a = 0; a < c.varActivityWeights.length; a++) c.varActivityWeights[a] = Double.parseDouble(v[5 + a]);
    c.varHome = g;
    g.colResidents.add(c);
    funPlaceIcon(c, g);
}

// 4. Links: saved impedance (replaces funInitImpedance's draw) and flow, so the first assignment warm-starts
for (String[] v : savedLinks) {
    int ka = Integer.parseInt(v[1]), kb = Integer.parseInt(v[2]);
    Link l = funFindLink(varGrid[ka / parCols][ka % parCols], varGrid[kb / parCols][kb % parCols]);
    l.varImpedance = Double.parseDouble(v[3]);
    l.funUpdateCapacity();
    l.varFlowVol = Double.parseDouble(v[4]);
    l.funUpdateTravelTime();
}

varInitialising = false;         // the city is already settled: no initialisation phase
varStateSaved = true;            // already on file: don't save it again
varAtLoadedEquilibrium = true;   // first checkpoint goes straight to investment
traceln("Saved state loaded: " + activities.size() + " activities, " + citizens.size() + " citizens, " + savedLinks.size() + " links");
return true;
