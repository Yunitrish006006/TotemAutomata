## 1. Shared Core contract

- [x] 1.1 Add a null-safe `TotemWorldOutlines.line` helper using the existing
  style and explicit depth/through-wall behavior.
- [x] 1.2 Unit-test line endpoints, colour, width and both occlusion modes.
- [x] 1.3 Document the added stateless line primitive in Core API docs.

## 2. Automata rendering

- [x] 2.1 Submit source and sorting-destination lines only for a valid selected
  Golem snapshot in the current dimension.
- [x] 2.2 Use depth-tested orange/green/red styles for available source,
  available destination and unavailable container relationships.
- [x] 2.3 Remove only the superseded source/destination particle chains while
  retaining gathering-target and blocked-state particles.
- [x] 2.4 Follow the selected client Copper Golem's interpolated position every
  render frame while retaining the server snapshot position as a fallback.

## 3. Verification

- [x] 3.1 Unit-test sorting/gathering mode, availability colours,
  different-dimension filtering and no always-on-top link.
- [x] 3.2 Client GameTest partly occluded container links and inspect a
  native-scale screenshot proving solid segments do not render through a wall.
- [x] 3.3 Run Core and Automata unit/build, server GameTest, Client GameTest and
  standalone runtime checks.
- [x] 3.4 After commit authorization, update Automata CI/release pins to the
  real Core implementation commit, then pass clean Production Runtime and
  release gates.
- [x] 3.5 Client GameTest a moved Copper Golem and inspect the updated
  native-scale line screenshot.

## 4. Documentation

- [x] 4.1 Document solid depth-tested container links and their colours in the
  Automata README.
- [x] 4.2 Strictly validate this OpenSpec change and record only executed
  evidence.
