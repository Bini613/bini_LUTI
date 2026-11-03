List<GridPoint> free = new ArrayList<>();   // points not yet holding any agent
for (GridPoint g : gridPoints) {
    if (!g.colLinks.isEmpty()) free.add(g);   // skip nodes with no links (inaccessible)
}

// Stop if there aren't enough accessible grid points for one agent each
if (parNumActivities + parNumCitizens > free.size()) {
    error("Too many agents: " + (parNumActivities + parNumCitizens) + " agents, but only " + free.size() + " accessible grid points");
}

// Pick and remove a random free point, biased towards the centre by negative exponential weight exp(-d / lambda),
// where d is the straight-line distance to the centre node in grid steps
int ci = parCols / 2;
int cj = parCols / 2;
java.util.function.DoubleFunction<GridPoint> pickNearCentre = lambda -> {
    double[] w = new double[free.size()];
    double total = 0;
    for (int k = 0; k < free.size(); k++) {
        GridPoint g = free.get(k);
        double d = sqrt(pow(g.parXIndex - ci, 2) + pow(g.parYIndex - cj, 2));
        w[k] = exp(-d / lambda);
        total += w[k];
    }
    double r = uniform(0, total);   // random point along the stacked weights
    int k = 0;
    while (k < free.size() - 1 && r >= w[k]) {   // walk along until r falls inside a node's share
        r -= w[k];
        k++;
    }
    return free.remove(k);
};

// Activities: each on its own free point, biased towards the centre
for (int k = 0; k < parNumActivities; k++) {
    GridPoint g = pickNearCentre.apply(parActivitiesInitialSpread);
    Activity a = add_activities(); //this method creates a new instance of an Activity agent
    a.varLocation = g;
    a.setXY(g.getX(), g.getY());
}

// Citizens: each on its own free point, biased towards the centre
for (int k = 0; k < parNumCitizens; k++) {
    GridPoint g = pickNearCentre.apply(parCitizensInitialSpread);
    Citizen c = add_citizens();
    c.varHome = g;
    c.setXY(g.getX(), g.getY());
}

traceln("Activities placed: " + activities.size() + ", citizens placed: " + citizens.size() + ", available nodes remaining: " + free.size());   // TEST