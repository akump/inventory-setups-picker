# Inventory Setups Picker

A companion to the [Inventory Setups](https://github.com/dillydill123/inventory-setups) plugin that lets you open
your setups from inside the game instead of the side panel.

<img src="docs/sections.gif" alt="The hotkey popup listing setups under their sections, then filtered by a section's name and by setup name" width="420">

## Hotkey popup

Press **Cmd+K** (Mac) or **Ctrl+K** (Windows/Linux) anywhere in game to open a list of all your setups. The key
can be changed, and the modifier turned off, in the plugin's settings. If you use the
[Platform Keys](https://github.com/akump/platform-keys) plugin, the modifier follows its key profile, so the
shortcut matches its bank search.

| Key | Action |
| --- | --- |
| Type | Filter setups by name |
| `Up` / `Down`, `Tab` / `Shift+Tab` | Move the selection |
| `Page Up` / `Page Down` | Move a page at a time |
| `Enter` | Open the selected setup (or close it, if it's the one already open) |
| `Esc`, the hotkey again, or clicking elsewhere | Cancel |

While the popup is open, typing goes to it rather than to the chatbox.

<img src="docs/popup.png" alt="The hotkey popup filtered to setups matching &quot;sla&quot;" width="700">

## Beside the bank

While the bank is open the same list is shown next to it. Click a setup to open it, click the open setup to close
it, click the search box to filter, scroll with the mouse wheel, and click the title bar to collapse the list. This
can be turned off in the plugin's settings.

<img src="docs/bank-list.png" alt="The list of setups beside the bank, with the open setup highlighted" width="700">

## Sections

If you sort your setups into sections in Inventory Setups, both lists group them the same way: a heading per
section, in the side panel's order, with the setups that aren't in any section under "Unassigned". A setup that is
in several sections is listed under each. Typing a section's name lists all of its setups. This can be turned off
in the plugin's settings to get one flat list.

With **Sections as pages** turned on in the settings, the lists show just the sections instead. Click a section, or
select it and press `Enter`, to see its setups on a page of their own, and click its name at the top, or press
`Left` or `Backspace`, to go back. Typing on a section's page searches that section; typing in the list of sections
still searches every setup.

## Requirements

Inventory Setups must be installed and enabled. Setups are listed alphabetically,
or in Inventory Setups' own order if you turn that off (optionally favorites first), with the icon and color you gave them there.

## Development

`./gradlew runClient` starts RuneLite with the plugin loaded. `./gradlew test` also writes a rendering of both
views to `build/preview.png`.
