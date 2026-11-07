// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funBuildDensitySurface).
// Runs one activity relocation round: every activity takes its turn with funEvaluateAndMoveActivity (choose, store values, move or stay).
// funEvaluateAndMoveActivity itself prints activity 0's utility terms at its location and its choice.
// Then prints how many moved, their average move distance, and how many activities share a node with another activity.
int movedActs = 0;
double sumActMoveDist = 0;
for (Activity act : activities) {
    GridPoint from = act.varLocation;
    if (funEvaluateAndMoveActivity(act)) {
        GridPoint to = act.varLocation;   // already moved if the condition was true
        movedActs++;
        sumActMoveDist += sqrt(pow(to.parXIndex - from.parXIndex, 2) + pow(to.parYIndex - from.parYIndex, 2)) * parStepLength;
    }
}
Set<GridPoint> actNodes = new HashSet<>();
for (Activity act : activities) actNodes.add(act.varLocation);
traceln("Activity relocation round: " + movedActs + " of " + activities.size() + " activities moved, average move "
        + (movedActs > 0 ? roundToDecimal(sumActMoveDist / movedActs, 0) : 0) + " m; activities sharing a node: " + (activities.size() - actNodes.size()));   // TEST
