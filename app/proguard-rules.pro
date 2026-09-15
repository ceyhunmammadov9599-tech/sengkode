# SENGKODE release rules.
#
# Privacy-first contract: no network code, no analytics, no logging of
# user content. R8 minification is ON from Phase 0; every phase gate
# re-verifies assembleRelease.

# Hilt, Room, Compose and Navigation ship consumer rules; nothing
# extra is required for the Phase 0 surface. Phase 1 will add the
# ZXing + serialization keeps when the engine lands.

-dontwarn org.codehaus.mojo.animal_sniffer.*
