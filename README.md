<!-- omit in toc -->
# Harpocrates

> She saw before her bed, or seemed to see as in a dream, great Isis with her train of holy deities. Upon her brow there stood the crescent moon-horns, garlanded with glittering heads of golden grain, and grace of royal dignity; and at her side... the god who holds his finger to his lips for silence's sake."

Harpocrates is a browser plugin that enhances bookmarks. For more details, head to [this section](#features).

| Branch | Status |
| ---- | ---- |
| master | [![CircleCI](https://circleci.com/gh/aratare-tech/harpocrates/tree/master.svg?style=svg)](https://circleci.com/gh/aratare-tech/harpocrates/tree/master) |
| develop | [![CircleCI](https://circleci.com/gh/aratare-tech/harpocrates/tree/develop.svg?style=svg)](https://circleci.com/gh/aratare-tech/harpocrates/tree/develop) |

<!-- omit in toc -->
## Table of Contents
- [Installation](#installation)
- [Features](#features)
- [Development](#development)
  - [Client](#client)
  - [Server](#server)

## Installation
TBC

## Features
- Bookmarks
- Folders
- Tags
- Passwords for folders
- Search/Filter

## Development
### Client
`lein fig -- -b dev -r`

### Server
1. Need `dev-config.edn`

```clojure
{:dev true
 :port 3000
 ;; when :nrepl-port is set the application starts the nREPL server on load
 :nrepl-port 7000
 :database-url "jdbc:postgresql://localhost:5432/harpocrates?user=postgres&password=123456789"}
 ```
 2. `lein run`