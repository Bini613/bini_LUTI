// Function in Main. Returns value, type: boolean. No arguments. Called by funRunCheckpoint at each density equilibrium.
// PLACEHOLDER: the investment strategy goes here (last build stage). It should change link impedances only through
// funApplyImpedanceChange, and never upgrade a link whose end nodes would then exceed the road-land limit.
// Returns true if it made any investment, false if nothing was affordable or allowed (budget used up, or every
// candidate blocked), which ends the run. A starting budget of 0 therefore gives the no-investment baseline.
return false;   // placeholder: no investment yet
