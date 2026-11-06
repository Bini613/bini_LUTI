// Function in Main. Just action (returns nothing). Arguments: ag (Agent), g (GridPoint)
// Display only: puts agent ag's icon at a random spot near node g instead of exactly on it, so agents sharing a node stay visible.
// The spot is uniform within a circle of radius parIconJitter * parSpacing around the node (parIconJitter = 0 puts it back on the node).
// Uses its own random generator (Main variable varDisplayRandom, type java.util.Random), not the model's,
// so icon positions never change the simulation's random draws: results are identical with or without jitter.
double radius = parIconJitter * parSpacing * sqrt(varDisplayRandom.nextDouble());   // sqrt spreads points evenly over the circle's area, instead of bunching them at the centre
double angle = 2 * PI * varDisplayRandom.nextDouble();
ag.setXY(g.getX() + radius * cos(angle), g.getY() + radius * sin(angle));
