#!/bin/bash

# Script to install KiteUI Claude skill globally
# This copies the skill from the project to ~/.claude/skills/

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the script's directory (handles being called from anywhere)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Source and destination paths
SOURCE_SKILL="$PROJECT_ROOT/.claude/skills/kiteui.md"
DEST_DIR="$HOME/.claude/skills"
DEST_SKILL="$DEST_DIR/kiteui.md"

echo -e "${BLUE}Installing KiteUI Claude skill globally...${NC}"

# Check if source file exists
if [ ! -f "$SOURCE_SKILL" ]; then
    echo "Error: Source skill file not found at $SOURCE_SKILL"
    exit 1
fi

# Create destination directory if it doesn't exist
if [ ! -d "$DEST_DIR" ]; then
    echo "Creating skills directory at $DEST_DIR"
    mkdir -p "$DEST_DIR"
fi

# Copy the skill file
echo "Copying skill from $SOURCE_SKILL"
echo "             to $DEST_SKILL"
cp "$SOURCE_SKILL" "$DEST_SKILL"

echo -e "${GREEN}✓ KiteUI skill installed successfully!${NC}"
echo ""
echo "You can now use the skill in any project with:"
echo "  /skill kiteui"
