// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funBuildDensitySurface).
// Runs one activity relocation round: every activity chooses (funChooseActivityLocation) and moves (funMoveActivity) in turn.
// funChooseActivityLocation itself prints activity 0's utility terms at its location and its choice.
// Then prints how many moved, their average move distance, and checks that no two activities share a node.
int movedActs = 0;
double sumActMoveDist = 0;
for (Activity act : activities) {
    GridPoint from = act.varLocation;
    GridPoint to = funChooseActivityLocation(act);
    if (to != from) {
        movedActs++;
        sumActMoveDist += sqrt(pow(to.parXIndex - from.parXIndex, 2) + pow(to.parYIndex - from.parYIndex, 2)) * parStepLength;
        funMoveActivity(act, to);
    }
}
Set<GridPoint> actNodes = new HashSet<>();
for (Activity act : activities) actNodes.add(act.varLocation);
traceln("Activity relocation round: " + movedActs + " of " + activities.size() + " activities moved, average move "
        + (movedActs > 0 ? roundToDecimal(sumActMoveDist / movedActs, 0) : 0) + " m; extra activities on already-occupied nodes: " + (activities.size() - actNodes.size()) + " (should be 0)");   // TEST
