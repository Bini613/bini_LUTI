// Function in Main. Returns value, type: double. Arguments: a (GridPoint), b (GridPoint)
// Probability multiplier for a potential new link a-b, based on the turn it makes at each end

// One end: node n, for a new link n-other, based on the turn it makes from n's single existing link.
// Only chains of links care about angles: nodes with 0 or 2+ links return 1.
java.util.function.ToDoubleBiFunction<GridPoint, GridPoint> oneEnd = (n, other) -> {
    if (n.colLinks.size() != 1) return 1;
    Link e = n.colLinks.get(0);
    GridPoint m = (e.parNodeA == n) ? e.parNodeB : e.parNodeA;   // node at the far end of the existing link
    // Dot product of the two directions, both pointing out from n
    int dot = (m.parXIndex - n.parXIndex) * (other.parXIndex - n.parXIndex)
            + (m.parYIndex - n.parYIndex) * (other.parYIndex - n.parYIndex);
    if (dot < 0) return 1;                        // straight on or 45 degree turn
    if (dot == 0) return parSharpTurnBias;        // right-angle turn
    return parSharpTurnBias * parSharpTurnBias;   // acute angle (worst)
};

return oneEnd.applyAsDouble(a, b) * oneEnd.applyAsDouble(b, a);