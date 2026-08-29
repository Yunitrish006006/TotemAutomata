## ADDED Requirements

### Requirement: Automata owns its Observer rendering
Automata SHALL reconstruct Copper Golem observation with its production `CopperGolemMenuScreen` and SHALL NOT require TotemVanillaTweaks to draw an approximation.

#### Scenario: Compatible provider
- **WHEN** an observer receives a compatible `automata_copper_golem` semantic snapshot
- **THEN** Automata creates its production screen in read-only Observer mode

### Requirement: Observer mode is inert and private
The Observer screen MUST NOT send menu actions, lifecycle requests, API keys, tokens, prompts or unsent editor text.

#### Scenario: Viewer interacts with the screen
- **WHEN** the viewer clicks, types, scrolls or drags in an observed Copper Golem screen
- **THEN** no target mutation packet is sent and secret editor state remains absent
