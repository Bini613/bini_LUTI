// Function in Main. Returns value, type: double. Arguments: a (GridPoint), b (GridPoint)
// Probability multiplier for a potential new link a-b, based on the turn it makes at each end

// One end: node n, for a new link n-other.
//   - 0 links: no angle to judge, returns 1.
//   - 1 link (a chain): based on the turn the new link makes from that link.
//   - 2+ links (the new link makes a junction): penalised for each existing link it would sit at 45 degrees to
//     (on a grid, a positive dot product between two different links always means 45 degrees). One such neighbour
//     gives parSharpTurnBias, two (a sandwich) gives its square; the exponent is also multiplied by
//     (nLinks - 1), so the penalty is stronger for 4-way and larger junctions.
java.util.function.ToDoubleBiFunction<GridPoint, GridPoint> oneEnd = (n, other) -> {
    int nLinks = n.colLinks.size();
    if (nLinks == 0) return 1;
    if (nLinks >= 2) {
        int narrow = 0;   // existing links at 45 degrees to the new one
        for (Link l : n.colLinks) {
            GridPoint m = (l.parNodeA == n) ? l.parNodeB : l.parNodeA;
            int dot = (m.parXIndex - n.parXIndex) * (other.parXIndex - n.parXIndex)
                    + (m.parYIndex - n.parYIndex) * (other.parYIndex - n.parYIndex);
            if (dot > 0) narrow++;
        }
        return pow(parSharpTurnBias, narrow * (nLinks - 1));   // same parameter as for sharp turns in chains
    }
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