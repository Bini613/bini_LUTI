// Function in Main. Returns value, type: double. Arguments: a (GridPoint), b (GridPoint)
// Probability multiplier for a potential new link a-b, based on how many links each end already has

// One end (node n), based on how many links n already has
java.util.function.ToDoubleFunction<GridPoint> oneEnd = n -> {
    int d = n.colLinks.size();
    if (d <= 1) return 1; //No penalty for an empty node or a dead end: extending a road onto an empty node is unpenalised
    return parJunctionFormationBias * pow(d - 1, -parAdditionalJunctionDecay);
};

double factor = oneEnd.applyAsDouble(a) * oneEnd.applyAsDouble(b);
// New stub in the middle of nowhere: both ends empty. Penalised once, here, so it doesn't affect road extensions
if (a.colLinks.isEmpty() && b.colLinks.isEmpty()) factor *= parNewStubProb;
return factor;