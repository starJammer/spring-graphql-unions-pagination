#!/usr/bin/env zsh

if [ -d "$HOME/.sdkman" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
else
    echo 'SDKMAN is not installed'
    echo 'Please install SDK man from: https://sdkman.io and then try again'
    exit 1
fi

# Install java if .sdkmanrc exists and sdk command is available
if [ -f ".sdkmanrc" ] && command -v sdk &> /dev/null; then
    sdk env install
fi
