List<GridPoint> free = new ArrayList<>();   // points not yet holding any agent
for (GridPoint g : gridPoints) {
    if (!g.colLinks.isEmpty()) free.add(g);   // skip nodes with no links (inaccessible)
}

// Stop if there aren't enough accessible grid points for one agent each
if (parNumActivities + parNumCitizens > free.size()) {
    error("Too many agents: " + (parNumActivities + parNumCitizens) + " agents, but only " + free.size() + " accessible grid points");
}

// Activities: each on its own random free point
for (int k = 0; k < parNumActivities; k++) {
    GridPoint g = free.remove(uniform_discr(0, free.size() - 1));   // this expression removes the index (random here) from the collection 'free', and also returns whatever was just removed for assignment to 'g'
    Activity a = add_activities(); //this method creates a new instance of an Activity agent
    a.varLocation = g;
    a.setXY(g.getX(), g.getY());
}

// Citizens: each on its own random free point
for (int k = 0; k < parNumCitizens; k++) {
    GridPoint g = free.remove(uniform_discr(0, free.size() - 1));
    Citizen c = add_citizens();
    c.varHome = g;
    c.setXY(g.getX(), g.getY());
}

traceln("Activities placed: " + activities.size() + ", citizens placed: " + citizens.size() + ", empty points left: " + free.size());   // TEST