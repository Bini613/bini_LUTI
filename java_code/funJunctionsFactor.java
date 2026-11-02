// Function in Main. Returns value, type: double. Arguments: a (GridPoint), b (GridPoint)
// Probability multiplier for a potential new link a-b, based on how many links each end already has

// One end (node n), based on how many links n already has
java.util.function.ToDoubleFunction<GridPoint> oneEnd = n -> {
    int d = n.colLinks.size();
    if (d == 0) return parConnectToNakedNodeBias;   // new link would make n a dead end
    return pow(parJunctionFormationBias, d - 1);   // 1 for d=1 (extending a road), then bias, bias^2, ...: falls with each extra link but never reaches 0
};

return oneEnd.applyAsDouble(a) * oneEnd.applyAsDouble(b);