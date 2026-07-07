
# Sync edits made in buildSrc back to their source modules.
#
# buildSrc mirrors both gradle-plugin and build-companion sources at configuration time.
# When editing in buildSrc for live IDE feedback, run this to push changes back to the
# correct source module. The autoroute/parsing helpers live in build-companion, NOT in
# gradle-plugin — this script keeps them separate.

# Companion sources: generateRoutes.kt, parsingHelpers.kt, TabAppendable.kt
COMPANION_FILES="generateRoutes.kt parsingHelpers.kt TabAppendable.kt"

rm -rf gradle-plugin/src/main/kotlin/
cp -r buildSrc/src/main/kotlin gradle-plugin/src/main/kotlin/

# Move the companion files out of the plugin and into build-companion
mkdir -p build-companion/src/main/kotlin/
for f in $COMPANION_FILES; do
    if [ -f "gradle-plugin/src/main/kotlin/$f" ]; then
        mv "gradle-plugin/src/main/kotlin/$f" "build-companion/src/main/kotlin/$f"
    fi
done
