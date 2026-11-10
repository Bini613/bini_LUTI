// Function in Main. Returns value, type: double. Arguments: a (GridPoint), b (GridPoint)
// Probability multiplier for a potential new link a-b, based on the situation it creates, in terms of chains.
// Each end is naked (0 links), a chain end (1 link) or a junction (2+ links, so the new link makes it 3-way or more).
// Angles along chains are handled separately by funAngleFactor.

int dA = a.colLinks.size();
int dB = b.colLinks.size();
double factor;

if (dA == 0 && dB == 0) {
    // 1. Both ends naked: a new stub in the middle of nowhere
    factor = parNewStubProb;
}
else if ((dA == 0 && dB == 1) || (dA == 1 && dB == 0)) {
    // 2a. One end naked, the other a chain end: the chain grows
    factor = 1;
}
else if (dA == 1 && dB == 1) {
    // 2b. Both ends chain ends: two chains join into one
    factor = 1;
}
else if ((dA == 0 && dB >= 2) || (dA >= 2 && dB == 0)) {
    // 3. One end a junction, the other naked: a new branch starts from the junction
    factor = parJunctionFormationBias;
}
else if ((dA == 1 && dB >= 2) || (dA >= 2 && dB == 1)) {
    // 4. One end a junction, the other a chain end: a chain attaches to the junction
    factor = parJunctionAttachmentBias;
}
else {
    // 5. Both ends junctions: a link between two junctions
    factor = parJunctionFormationBias * parJunctionFormationBias;
}

// Narrow junctions: at each end with 2+ links, penalise for every existing link which the new one would sit at 45 degrees next to
// (on a grid, a positive dot product between two different links always means 45 degrees). The exponent is also
// multiplied by (links - 1), so two 45-degree neighbours cost more at a 4-link node than at a 3-link one.
java.util.function.ToDoubleBiFunction<GridPoint, GridPoint> narrowPenalty = (n, other) -> {
    int nLinks = n.colLinks.size();
    if (nLinks < 2) return 1;   // not a junction: chain angles are funAngleFactor's job
    int narrow = 0;   // existing links at 45 degrees to the new one
    for (Link l : n.colLinks) {
        GridPoint m = (l.parNodeA == n) ? l.parNodeB : l.parNodeA;
        int dot = (m.parXIndex - n.parXIndex) * (other.parXIndex - n.parXIndex)
                + (m.parYIndex - n.parYIndex) * (other.parYIndex - n.parYIndex);
        if (dot > 0) narrow++;
    }
    return pow(parNarrowBias, narrow * (nLinks - 1));
};
if (dA >= 2 || dB >= 2) {
    factor *= narrowPenalty.applyAsDouble(a, b) * narrowPenalty.applyAsDouble(b, a);
}

return factor;
