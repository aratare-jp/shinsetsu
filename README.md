# Shinsetsu 新設

> Noun, Suru verb, No-adjective
>
> 1. establishing; founding; setting up; organizing; establishment

Shinsetsu is a browser plugin that takes the bookmark pain away from you.

## Status
`Active development`

[![Clojure CI](https://github.com/aratare-jp/shinsetsu/actions/workflows/clojure.yml/badge.svg)](https://github.com/aratare-jp/shinsetsu/actions/workflows/clojure.yml)
[![codecov](https://codecov.io/gh/aratare-jp/shinsetsu/branch/main/graph/badge.svg?token=PAKAKHIBEI)](https://codecov.io/gh/aratare-jp/shinsetsu)

## Installation

TBC

## Features

- [ ] One-stop command line interface for all operations
- [ ] Standard bookmark operations (minus a few things)
- [ ] Cross-browser bookmark sync
- [ ] Tagging for easy searching & filtering
- [ ] Saved sessions for easy retrieval of tabs
- [ ] Securing with passwords
- [ ] Self-hosted server

## Rationale

As we progress through the information age, our ability to organise information effectively seems to stay the same. For
example, have you ever found a really nice website about cooking and thought

> I'll just bookmark this website just in case I need it in the future

and _never_ touched it again? I sure have. In fact, all the time. But, why?

Well, the answer is simple: that website has disappeared among _many_ other websites, all of which were the results of
the "I'll just bookmark this website just in case" habit. Solution? Either:

1. Relying on autocompletion to find that website via the address bar (which is a hit-or-miss most of the time), or
2. Summoning up the bookmark manager. 

Either way, chances are you can find it, but not without having to go through all these 
unnecessary steps. Oh, and good luck trying to do so  with a _nested structure_ (i.e. bookmark folders within bookmark 
folders) that a majority of us, when feel the need to "organise" our bookmarks, seem to always fall back on.

As my bookmarks grow each day, and finding things has become such a chore that I truly need a better way to tame this
beast. And so I started finding a solution that suits my need, or building one if I have to. I did the latter.

Enter Shinsetsu - a standalone bookmark "stack" - designed to replace the stock standard bookmark managers.

Shinsetsu aims to be as seamless as possible, in the sense that if you need to do something, you can do so
_quickly_. You can find its features in the section above, but here's a quick sneakpeek:

- Instead of having nested structure, Shinsetsu promotes _tabs_ and _tags_.
    * Tabs are the highest level of aggregation, i.e. your "cooking" bookmark folder. You use tabs to put bookmarks
into groups that represents your interests, e.g. cooking, coding, learning, etc. Think of tabs as sections within a
library, e.g. History, Culture, Literature, etc.
    * For finer organisation, you can use tags. Tags are used to cross-label things across multiple tabs. Want to
find that Italian cooking website about pasta? One way to organise such website would be to put it in "cooking" tab
with "Italian" and "Pasta" tags. It's like searching for a book in a library: you walk through sections (tab) and go
through the alphabetically labeled shelves (tags) within the section.
- Secret sections in a library are cool, and so are protected tabs. You can assign passwords to tabs to protect
sensitive data, such as work-related documentations.
- Saved sessions allow you to open a certain set of tabs easily. And yes: it can also be password-protected.
- Cross-browser synchronisation so no longer do you need to maintain separate accounts for different browsers. Add a
bookmark in Firefox and access it in Chrome with ease.
- And many more...

My goal, as stated, is to be able to use Shinsetsu to make actual good use of bookmarks. And I hope it will be able
to bring joy to you as well.

# Progress (for tracking)

- [ ] Login
- [ ] Tab
- [ ] Bookmark
- [ ] Chrome plugin

# FAQ
## Is it free?
Yes.

## Will it be free _forever_?
I can guarantee the core functionality will **_always_** be free. If I happen to build some additional life-enhancing features in the future, then I'll make the decision then.

## Is it safe?
Yes. In addition to be password-protected, all the data is saved on your _own_ server. What's yours remain yours.

## How easy is it to setup?
Currently the project is in development so I can't tell you much _yet_. But I'm striving to make it as easy as possible.

## Will it be cross-platform?
Currently bookmarks are mostly used in browser environment, so probably not for the foreseeable future. Once the browser plugin is mature enough then _maybe_ I'll reconsider.

# Contribution

This project for the most part is for my personal use. If you've found any bugs or issues, or want to have
additional features, please create an issue or PR.

# License
MIT License

Copyright (c) 2020 Rex Truong

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
