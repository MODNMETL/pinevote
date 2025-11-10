# PineVote

Lightweight, production-ready **yes/no voting** plugin for **PaperMC 1.21.x** — built and maintained by **MODN METL LTD**.  
Includes **SQLite** storage, **PlaceholderAPI** support, and **configurable IP-bypass permissions** (for shared households).

![CI](https://github.com/MODNMETL/pinevote/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-007396)
![Paper](https://img.shields.io/badge/Paper-1.21.x-blue)
![Release](https://img.shields.io/github/v/release/MODNMETL/pinevote?display_name=tag)

---

## Table of Contents
- [Features](#features)
- [Requirements](#requirements)
- [Install](#install)
- [Configuration](#configuration)
- [Permissions](#permissions)
- [Commands](#commands)
- [Placeholders (PlaceholderAPI)](#placeholders-placeholderapi)
- [Build Locally](#build-locally)
- [Versioning & Releases](#versioning--releases)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Support](#support)
- [Security](#security)
- [License](#license)
- [Credits](#credits)

---

## Features
- `/pinevote yes` or `/pinevote no` — cast an **anonymous** vote (1 per UUID).
- `/pinevote status` — displays current YES/NO tallies.
- `/pinevote reset` — **admin** resets all votes.
- **IP duplicate prevention** with a **configurable bypass permission** (useful for siblings/households sharing one IP).
- **SQLite** persistence with WAL mode for performance.
- **PlaceholderAPI** expansion: `%pinevote_yes%`, `%pinevote_no%`, `%pinevote_total%`.
- Graceful background DB operations; cached tallies auto-refresh.

---

## Requirements
- **Java 21**
- **Paper** 1.21.x
- *(Optional)* **PlaceholderAPI** (for placeholders)

> Run your Paper server with Java 21, or you’ll see “unsupported class version” errors.

---

## Install
1. Download the latest JAR from **[Releases](../../releases)**.
2. Drop the JAR into your server’s `plugins/` directory.
3. Start (or restart) the server.
4. The plugin will create:
    - `plugins/PineVote/config.yml` (configuration)
    - `plugins/PineVote/pinevote.db` (SQLite database)

---

## Configuration
`plugins/PineVote/config.yml` is created on first run. Default:

```yaml
# ================================
# PineVote Configuration
# ================================

# Messages sent to players
messages:
  voted_yes: "Thanks — your YES vote has been recorded."
  voted_no: "Thanks — your NO vote has been recorded."
  already_voted: "You have already voted."
  duplicate_ip: "A vote from your location has already been recorded."
  reset_done: "All votes reset."

# How often to refresh cached counts (in seconds)
cache:
  refresh_seconds: 30

# Logging behaviour
logging:
  audit_votes_to_console: true

# Permission node which allows bypassing the IP-based duplicate check.
# Players with this permission can still only vote once per UUID.
permissions:
  bypass_node: "noaltsexploits.bypass"
