## 1. Fuel lifecycle

- [ ] 1.1 Replace fixed transaction fuel debit with an authoritative
  executable-work-cycle tick hook shared by sorting and gathering.
- [ ] 1.2 Persist original burn duration with remaining burn ticks, including
  safe legacy-data handling and crafting remainders.
- [ ] 1.3 Preserve no-fuel, stopped, idle and blocked behaviour without
  burning queued fuel.

## 2. Menu state and rendering

- [ ] 2.1 Extend the authoritative Copper Golem snapshot with burn duration.
- [ ] 2.2 Render the fuel slot, lit/unlit furnace meter and accessible numeric
  status in the accepted production layout.
- [ ] 2.3 Wire the mode and LLM controls into that accepted layout without
  changing their existing authorities.

## 3. Verification

- [ ] 3.1 Add GameTests for active tick draining, no burn while idle/blocked,
  refill handoff, remainders and restart persistence.
- [ ] 3.2 Update client visual GameTests for full, partial, empty and legacy
  fuel states.
- [ ] 3.3 Run the unit suite, server GameTests, client GameTests and strict
  OpenSpec validation.
