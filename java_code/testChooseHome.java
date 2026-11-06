// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funCalibrateDensity and funAssignTraffic).
// Runs one relocation round: every citizen chooses (funChooseHome) and moves (funMoveCitizen) in turn.
// funChooseHome itself prints citizen 0's utility terms at home and its choice.
// Then prints how many moved, their average move distance, and checks that no two citizens share a node.
int moved = 0;
double sumMoveDist = 0;
for (Citizen c : citizens) {
    GridPoint from = c.varHome;
    GridPoint to = funChooseHome(c);
    if (to != from) {
        moved++;
        sumMoveDist += sqrt(pow(to.parXIndex - from.parXIndex, 2) + pow(to.parYIndex - from.parYIndex, 2)) * parStepLength;
        funMoveCitizen(c, to);
    }
}
Set<GridPoint> homes = new HashSet<>();
for (Citizen c : citizens) homes.add(c.varHome);
traceln("Relocation round: " + moved + " of " + citizens.size() + " citizens moved, average move " + (moved > 0 ? roundToDecimal(sumMoveDist / moved, 0) : 0)
        + " m; shared homes: " + (citizens.size() - homes.size()) + " (should be 0)");   // TEST
