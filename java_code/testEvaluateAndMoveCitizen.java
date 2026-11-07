// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funBuildDensitySurface and funAssignTraffic).
// Runs one relocation round: every citizen takes its turn with funEvaluateAndMoveCitizen (choose, store values, move or stay).
// funEvaluateAndMoveCitizen itself prints citizen 0's utility terms at home and its choice.
// Then prints how many moved, their average move distance, and how many citizens share a node with another citizen.
int moved = 0;
double sumMoveDist = 0;
for (Citizen c : citizens) {
    GridPoint from = c.varHome;
    if (funEvaluateAndMoveCitizen(c)) {
        GridPoint to = c.varHome;   // already moved if the condition was true
        moved++;
        sumMoveDist += sqrt(pow(to.parXIndex - from.parXIndex, 2) + pow(to.parYIndex - from.parYIndex, 2)) * parStepLength;
    }
}
Set<GridPoint> homes = new HashSet<>();
for (Citizen c : citizens) homes.add(c.varHome);
traceln("Relocation round: " + moved + " of " + citizens.size() + " citizens moved, average move " + (moved > 0 ? roundToDecimal(sumMoveDist / moved, 0) : 0)
        + " m; citizens sharing a node: " + (citizens.size() - homes.size()));   // TEST
