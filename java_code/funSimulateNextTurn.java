// Function in Main. Just action (returns nothing). No arguments. Called by the cyclic event evSimulateNextTurn.
// One turn: the next agent in varRelocationQueue chooses a location and moves there (or stays).
//   - Round handling: the queue position is varTurnCount % queue size. When it wraps back to 0, the round is over:
//     varRounds goes up, the run stops after parMaxRounds rounds, otherwise the queue is reshuffled for the next round.
//   - Citizen: funChooseHome, then funMoveCitizen.
//   - Activity: skips its turn with probability parProbActivityRelocationSkip; otherwise funChooseActivityLocation, then funMoveActivity.
//   - Only actual moves count towards varRelocationsSinceAssignment (skips and stays change nothing).
//   - Traffic is re-assigned when that count reaches parRelocationsPerAssignment, or straight after any activity move
//     (without re-assignment, an activity move makes its row of varAccessTimes / varAccessDistances point to its old location).
//   - Every parAssignmentsPerInvestment assignments, funInvest runs and traffic is assigned again, so the investment
//     both uses fresh equilibrium data and is reflected straight away. parAssignmentsPerInvestment = 0 switches investment off.
//     The assignment after an investment isn't counted in varAssignmentCount, so investments stay evenly spaced.

int queueSize = varRelocationQueue.size();
int pos = varTurnCount % queueSize;   // this turn's place in the queue
if (pos == 0 && varTurnCount > 0) {   // back at the start of the queue: a round has just finished
    varRounds++;
    funTestPrint("Round " + varRounds + " finished after " + varTurnCount + " turns");   // TEST
    if (varRounds >= parMaxRounds) {
        finishSimulation();
        return;
    }
    funShuffleResetRelocationQueue();   // new random order for the next round
}

Agent agent = varRelocationQueue.get(pos);
varTurnCount++;

boolean moved;
boolean isActivity = agent instanceof Activity;
if (isActivity) {
    Activity act = (Activity) agent;   // the queue holds both types as Agent, so it must be cast back to Activity to use its fields
    if (randomTrue(parProbActivityRelocationSkip)) return;   // skipped turn: nothing changes, nothing counted
    GridPoint to = funChooseActivityLocation(act);
    moved = (to != act.varLocation);
    funMoveActivity(act, to);   // does nothing if to is its current location
}
else {
    Citizen c = (Citizen) agent; //cast to Citizen class type
    GridPoint to = funChooseHome(c);
    moved = (to != c.varHome);
    funMoveCitizen(c, to);   // does nothing if to is its current home
}
if (!moved) return;   // stayed: nothing changed, so nothing counted

varRelocationsSinceAssignment++;
if (isActivity || varRelocationsSinceAssignment >= parRelocationsPerAssignment) {
    funAssignTraffic();
    varAssignmentCount++;
    varRelocationsSinceAssignment = 0;
    funTestPrint("Turn " + varTurnCount + ": assignment " + varAssignmentCount + (isActivity ? " (activity moved)" : " (periodic)"));   // TEST
    testPrintDensity();   // Runtime TEST: current average density vs varDensityRef

    if (parAssignmentsPerInvestment > 0 && varAssignmentCount % parAssignmentsPerInvestment == 0) {
        funInvest();
        funAssignTraffic();   // re-equilibrate on the upgraded network (not counted, see header)
        funTestPrint("Turn " + varTurnCount + ": investment made after assignment " + varAssignmentCount);   // TEST
    }
}
