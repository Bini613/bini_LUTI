// Function in Main. Just action (returns nothing). No arguments. Called by the cyclic event evSimulateNextTurn.
// One turn: the next agent in varRelocationQueue chooses a location and moves there (or stays).
//   - Round handling: the queue position is varTurnCount % queue size. When it wraps back to 0, the round is over:
//     varRounds goes up and the queue is reshuffled for the next round.
//   - Citizen: funEvaluateAndMoveCitizen (chooses, stores its values, and moves or stays).
//   - Activity: skips its turn with probability parProbActivityRelocationSkip; otherwise funEvaluateAndMoveActivity (chooses, stores its values, and moves or stays).
//   - Only actual moves count towards varRelocationsSinceAssignment (skips and stays change nothing).
//   - Traffic is re-assigned when that count reaches parRelocationsPerAssignment, or straight after any activity move
//     (without re-assignment, an activity move makes its row of varAccessTimes / varAccessDistances point to its old location).
//   - Checkpoints (funRunCheckpoint: equilibrium check, then invest or end the run) are called from here, by turn count
//     (see MAIN ROUNDS below), replacing the old timed event evCheckpoint.

// INITIALISATION PHASE (runs before the main rounds; varTurnCount, varRounds and the density checks are not touched during it)
// Real cities don't start with activities and citizens placed arbitrarily far apart. So first every activity takes one turn
// (shuffled order) to move towards where the citizens are, then citizens take parInitialisationRounds full rounds
// (reshuffled each round) to respond to the activities and to each other. This settles the arbitrary starting layout
// before the main run, so the city doesn't swing back and forth like a released spring.
//   - Activities' choice doesn't use travel times (only rent and straight-line distance to citizens), so traffic is not
//     re-assigned between activity moves: one assignment after the last activity is enough. Activities always take their turn here
//     (parProbActivityRelocationSkip is not applied).
//   - Citizen moves re-assign traffic every parRelocationsPerAssignment moves, as in the main run.
//   - The phase ends with one more assignment, then the main rounds start on the next turn.
//   - The whole phase is skipped if varInitialising (Main variable, boolean) starts as false.
// Uses Main variables varInitialising (boolean, initial true), varInitTurn (int, initial 0) and varInitQueue (ArrayList<Agent>, initial null).
if (varInitialising) {
    int nAct = activities.size();
    int nCit = citizens.size();
    if (varInitQueue == null) {   // first call: build the whole initialisation order (activities once, then citizens parInitialisationRounds times)
        varInitQueue = new ArrayList<>();
        ArrayList<Agent> actOrder = new ArrayList<>();
        for (Activity a : activities) actOrder.add(a);
        Collections.shuffle(actOrder, getDefaultRandomGenerator());
        varInitQueue.addAll(actOrder);
        for (int r = 0; r < parInitialisationRounds; r++) {
            ArrayList<Agent> citOrder = new ArrayList<>();
            for (Citizen c : citizens) citOrder.add(c);
            Collections.shuffle(citOrder, getDefaultRandomGenerator());   // fresh order every round
            varInitQueue.addAll(citOrder);
        }
        funRuntimePrint("Initialisation: " + nAct + " activity turns, then " + parInitialisationRounds + " citizen rounds (" + varInitQueue.size() + " turns)");
    }

    if (varInitTurn >= varInitQueue.size()) {   // everyone has had their initialisation turns: finish the phase
        funAssignTraffic();
        varAssignmentCount++;
        varRelocationsSinceAssignment = 0;
        varInitialising = false;
        varInitQueue = null;   // no longer needed
        funRuntimePrint("Initialisation finished: main rounds start now");
        return;   // this turn is used for the final assignment
    }

    if (varInitTurn == nAct) {   // the last activity has just had its turn: update traffic once for all activity moves
        funAssignTraffic();
        varAssignmentCount++;
        varRelocationsSinceAssignment = 0;
        funRuntimePrint("Initialisation: all activities have had their turn; citizens now respond");
    }
    else if (varInitTurn > nAct && (varInitTurn - nAct) % nCit == 0) {   // a citizen round has just finished
        funRuntimePrint("Initialisation: citizen round " + ((varInitTurn - nAct) / nCit) + " of " + parInitialisationRounds + " finished");
    }

    Agent initAgent = varInitQueue.get(varInitTurn);
    varInitTurn++;
    if (initAgent instanceof Activity) {
        funEvaluateAndMoveActivity((Activity) initAgent);   // no re-assignment here (see above)
        return;
    }
    if (funEvaluateAndMoveCitizen((Citizen) initAgent)) {   // moved: count it towards the next periodic assignment
        varRelocationsSinceAssignment++;
        if (varRelocationsSinceAssignment >= parRelocationsPerAssignment) {
            funAssignTraffic();
            varAssignmentCount++;
            varRelocationsSinceAssignment = 0;
        }
    }
    return;
}

// MAIN ROUNDS
// Checkpoint (equilibrium check, then invest or end the run), counted in main-round turns rather than model time, so it
// falls after the same number of turns whatever the path (virgin or loaded) or event timing. The first one comes once
// parMinRounds full rounds have passed, then every half round. It runs before this turn's queue position is worked out,
// since a checkpoint may reset the turn counter and queue.
int turnsPerRound = parNumCitizens + parNumActivities;
int firstCheckpoint = parMinRounds * turnsPerRound;
int checkpointEvery = turnsPerRound / 2;
if (varTurnCount >= firstCheckpoint && (varTurnCount - firstCheckpoint) % checkpointEvery == 0) {
    traceln("Checkpoint at turn " + varTurnCount + ", after " + (varTurnCount / turnsPerRound) + " full rounds");   // varRounds only goes up later in this call, so it isn't used here
    funRunCheckpoint();
    if (varReportPrinted) return;   // the checkpoint ended the run
}

int queueSize = varRelocationQueue.size();
int pos = varTurnCount % queueSize;   // this turn's place in the queue
if (pos == 0 && varTurnCount > 0) {   // back at the start of the queue: a round has just finished
    varRounds++;
    funTestPrint("Round " + varRounds + " finished after " + varTurnCount + " turns");   // TEST
    funShuffleResetRelocationQueue();   // new random order for the next round
}

Agent agent = varRelocationQueue.get(pos);
varTurnCount++;

boolean moved;
boolean isActivity = agent instanceof Activity;
if (isActivity) {
    Activity act = (Activity) agent;   // the queue holds both types as Agent, so it must be cast back to Activity to use its fields
    if (randomTrue(parProbActivityRelocationSkip)) return;   // skipped turn: nothing changes, nothing counted
    moved = funEvaluateAndMoveActivity(act);   // Evaluates options for and possibly moves the activity, and returns true if it moved, false if it stayed
}
else {
    Citizen c = (Citizen) agent; //cast to Citizen class type
    moved = funEvaluateAndMoveCitizen(c);   // Actually evaluates options for and possibly moves the citizen, and returns true if it moved, false if it stayed
}
if (!moved) return;   // stayed: nothing changed, so nothing counted

varRelocationsSinceAssignment++;
if (isActivity || varRelocationsSinceAssignment >= parRelocationsPerAssignment) {
    funAssignTraffic();
    varAssignmentCount++;
    varRelocationsSinceAssignment = 0;
    funTestPrint("Turn " + varTurnCount + ": assignment " + varAssignmentCount + (isActivity ? " (activity moved)" : " (periodic)"));   // TEST
}
