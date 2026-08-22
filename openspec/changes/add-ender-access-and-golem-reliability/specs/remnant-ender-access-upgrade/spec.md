## ADDED Requirements

### Requirement: Ender Access upgrade

Remnant SHALL provide a unique Ender Access backpack upgrade with a shaped
recipe, translated name and description, creative-tab entry, strict 16x16 item
texture, and a server-synchronised recipe page in the Remnant manual. It SHALL
be installable in ordinary tiered Remnant backpacks and SHALL NOT change Death
Backpacks.

#### Scenario: Player obtains the upgrade recipe

- **WHEN** a player opens an up-to-date Remnant manual
- **THEN** the manual SHALL show the live server recipe and explain that the
  upgrade opens that player's own Ender Chest from the backpack

#### Scenario: Duplicate upgrade is inserted

- **WHEN** a backpack already contains Ender Access and the player attempts to
  install a second Ender Access upgrade
- **THEN** the existing unique-upgrade rule SHALL reject the duplicate

### Requirement: Server-owned Ender access

While Ender Access is installed, the backpack screen SHALL show a translated
vanilla-style action button. A successful click SHALL close the backpack menu
normally and open a three-row vanilla container backed directly by the
requesting player's own Ender Chest inventory.

#### Scenario: Installed module opens own inventory

- **WHEN** a player clicks the Ender Access button while the tracked backpack
  is still valid and still contains the upgrade
- **THEN** the server SHALL open that player's own Ender Chest inventory without
  copying its contents into backpack data

#### Scenario: Module is absent

- **WHEN** the open backpack does not contain Ender Access
- **THEN** the button SHALL not render and a stale or forged button action SHALL
  not open Ender storage

#### Scenario: Backpack state changes before the click

- **WHEN** the tracked backpack is no longer held or the upgrade was removed
  before the server handles the action
- **THEN** the server SHALL reject the action without exposing any Ender Chest
  inventory

#### Scenario: Crafting grid contains items

- **WHEN** a player opens Ender storage while the backpack crafting grid
  contains items
- **THEN** the normal backpack close lifecycle SHALL return those items exactly
  once before the Ender Chest menu opens
