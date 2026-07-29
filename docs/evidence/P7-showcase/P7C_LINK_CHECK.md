# P7C Link Check

The repository-integrity script validated README local Markdown links after the P7C README and architecture entries were written. It rejects missing local targets, `file:` URIs, Windows absolute paths, and personal LAN IP addresses in README.

GitHub repository links are allowed. Canonical screenshot paths are relative repository paths and are checked as files rather than hosted URLs.
