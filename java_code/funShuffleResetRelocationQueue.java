// Function in Main. Just action (returns nothing). No arguments.
// Rebuilds varRelocationQueue with every citizen and activity and then shuffles it
// The shuffle gives a fresh random order every round, so no part of the city always moves first (avoids locational herding).
// Shuffling uses AnyLogic's random generator, so runs with the same seed give the same order.
// Called at the end of Main's On startup, and by funSimulateNextTurn whenever the queue runs out.
varRelocationQueue = new ArrayList<>();
for (Citizen c : citizens) varRelocationQueue.add(c);   // populations can't be passed to addAll, so agents are added one at a time
for (Activity a : activities) varRelocationQueue.add(a);
Collections.shuffle(varRelocationQueue, getDefaultRandomGenerator());

// TEST: queue size and the type of the first 10 agents (citizens and activities should be mixed)
String s = "";
for (int i = 0; i < min(10, varRelocationQueue.size()); i++) s += (varRelocationQueue.get(i) instanceof Citizen ? "C " : "A ");
traceln("Relocation queue: " + varRelocationQueue.size() + " agents (" + citizens.size() + " citizens, " + activities.size() + " activities). First 10: " + s);   // TEST
