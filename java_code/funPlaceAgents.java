List<GridPoint> accessible = new ArrayList<>();   // nodes an agent can be placed on
for (GridPoint g : gridPoints) {
    if (!g.colLinks.isEmpty()) accessible.add(g);   // skip nodes with no links (inaccessible)
}
if (accessible.isEmpty()) error("No accessible grid points to place agents on");

// Pick a random accessible point, biased towards the centre by negative exponential weight exp(-d / lambda),
// where d is the straight-line distance to the centre node in grid steps.
// Points are not removed once picked, so several agents can share a node (any mix of citizens and activities).
int ci = parCols / 2;
int cj = parCols / 2;
java.util.function.DoubleFunction<GridPoint> pickNearCentre = lambda -> {
    double[] w = new double[accessible.size()];
    double total = 0;
    for (int k = 0; k < accessible.size(); k++) {
        GridPoint g = accessible.get(k);
        double d = sqrt(pow(g.parXIndex - ci, 2) + pow(g.parYIndex - cj, 2));
        w[k] = exp(-d / lambda);
        total += w[k];
    }
    double r = uniform(0, total);   // random point along the stacked weights
    int k = 0;
    while (k < accessible.size() - 1 && r >= w[k]) {   // walk along until r falls inside a node's share
        r -= w[k];
        k++;
    }
    return accessible.get(k);
};

// Activities: biased towards the centre
for (int k = 0; k < parNumActivities; k++) {
    GridPoint g = pickNearCentre.apply(parActivitiesInitialSpread);
    Activity a = add_activities(); //this method creates a new instance of an Activity agent
    a.funBuildActivityPreferences();   // draws VOM and VOL (straight after creation, so the random draws happen in the same order as before)
    a.varLocation = g;
    g.colActivities.add(a);   // GridPoint collection (ArrayList of Activity): every activity at a node
    funPlaceIcon(a, g);   // icon near the node (display only)
}

// Citizens: biased towards the centre
for (int k = 0; k < parNumCitizens; k++) {
    GridPoint g = pickNearCentre.apply(parCitizensInitialSpread);
    Citizen c = add_citizens();
    c.funBuildCitizenPreferences();   // draws activity weights, VOT, VOM and VOP (needs all activities created first, as they are above)
    c.varHome = g;
    g.colResidents.add(c);   // GridPoint collection (ArrayList of Citizen): everyone living at a node
    funPlaceIcon(c, g);   // icon near the node (display only)
}

// TEST: how many nodes ended up occupied, and the most agents on one node
int occupiedNodes = 0, maxAgents = 0;
for (GridPoint g : gridPoints) {
    int n = g.colResidents.size() + g.colActivities.size();
    if (n > 0) occupiedNodes++;
    maxAgents = max(maxAgents, n);
}
traceln("Activities placed: " + activities.size() + ", citizens placed: " + citizens.size()
        + ", occupied nodes: " + occupiedNodes + ", most agents on one node: " + maxAgents);   // TEST
