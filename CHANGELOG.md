updates feed: https://xyrio.github.io/RSSOwlnix-site/updates.rss

- updated share links: removed Mister Wong, Google+ and added BibSonomy [#95](https://github.com/Xyrio/RSSOwlnix/issues/95)
- fixed progress button at bottom left to show new Download & Activities view (eclipse ProgressView) toggleable from Menu/View/Downloads & Activity. Kept old view at Menu/Tools/Downloads & Activity for now. [#45](https://github.com/Xyrio/RSSOwlnix/issues/45)
- changed page size preference for newspaper/headlines view to be setable to any value [#91](https://github.com/Xyrio/RSSOwlnix/issues/91)
- fixed missing Overview/Network Connections in Preferrences [#67](https://github.com/Xyrio/RSSOwlnix/issues/67)

# 2.8.0-beta
- improved favico search
- updated httpclient to 4.5.12
- added shell:// protocol for feed links to retrieve or transform rss using external scripts or programs. example shell://python html2rss.py https://website/

# 2.7.1-beta
- runs also with java 13, 14
- fix null news in feed when doing cleanup [#64](https://github.com/Xyrio/RSSOwlnix/issues/64)
- fixed different behaviour when adding new feed from toolbar [#42](https://github.com/Xyrio/RSSOwlnix/issues/42)
- updated rssowl news to use RSSOwlnix's update.rss [PR#59](https://github.com/Xyrio/RSSOwlnix/pull/59)

# 2.7.0-beta
- runs also with java 12
- updated eclipse rcp to 4.9.1 (last rcp supporting 32bit) (no babel localization for 4.9+)
- Added Telegram to options for sharing links [#33](https://github.com/Xyrio/RSSOwlnix/issues/33) [PR#38](https://github.com/Xyrio/RSSOwlnix/pull/38)

# 2.6.1-beta
- fixed wrong sticky news counting when doing a cleanup [#22](https://github.com/Xyrio/RSSOwlnix/issues/22)

# 2.6.0-beta
- fixed some website authentication and authentication secure storage related bugs since updating httpclient to 4.x
- added support for feed links using https like `feed:https://host/rss.xml` ( https://en.wikipedia.org/wiki/Feed_URI_scheme )
- added right to left sorting for search dialog title column too

# 2.5.5-beta
- fixed type check for preferences

# 2.5.4-beta
- updated httpclient to 4.5.6

# 2.5.3-beta
- updated eclipse rcp to 4.7.3a

# 2.5.2-beta
- fixed missing 256x256 program logo icon [#14](https://github.com/Xyrio/RSSOwlnix/issues/14)

# 2.5.1-beta
- added right to left sorting for title column of classic view (appends "<-" to Title column)
- linux: do not force xulrunner [PR#21](https://github.com/Xyrio/RSSOwlnix/pull/21)

# 2.5.0-beta
- main program is now updateable (addons and translation available through Help/Install new Software...) * does not work correctly
- renamed to RSSOwlnix
- updated httpclient to 4.5.5
- updated eclipse rcp to 4.7.2

# 2.4.0-beta
- removed old update manager and added new p2 one (addons work so far)

# 2.3.0-beta
- runs also with java 9
- https websites which needed JCE should work without it when jre 9+ is used. https://sourceforge.net/p/rssowl/discussion/296910/thread/6dc4a203/
- updated eclipse rcp to 4.7
- updated httpclient to 4.5.3
- fixed 2 (maybe) memory leaks

based on RSSOwl 2.2.1
