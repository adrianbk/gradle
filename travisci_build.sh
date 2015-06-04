#!/usr/bin/env bash
if [ "$TRAVIS_PULL_REQUEST_FIXME" == "false" ]; then
  echo "==== This was not a pull request, nothing executed! ===="
else
  TERM=xterm ./gradlew $BUILD -S --continue --console plain
fi
