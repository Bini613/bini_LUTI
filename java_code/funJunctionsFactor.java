// Function in Main. Returns value, type: double. Arguments: a (GridPoint), b (GridPoint)
// Probability multiplier for a potential new link a-b, based on how many links each end already has

// One end (node n), based on how many links n already has
java.util.function.ToDoubleFunction<GridPoint> oneEnd = n -> {
    int d = n.colLinks.size();
    if (d == 0) return parConnectToNakedNodeBias;   // new link would make n a dead end
    return max(0.0, 1 - (1 - parJunctionFormationBias) * (d - 1));   // 1 for d=1 (extending a road), falling with each extra link
};

return oneEnd.applyAsDouble(a) * oneEnd.applyAsDouble(b);