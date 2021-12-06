#!/usr/bin/env bash

git submodule update --init --recursive

echo "<html><ul>" > src/main/resources/META-INF/change-notes.html
git log `git describe --tags --abbrev=0`..HEAD --no-merges --oneline --pretty=format:"<li>%h %s (%an)</li>" >> src/main/resources/META-INF/change-notes.html
echo "</ul></html>" >> src/main/resources/META-INF/change-notes.html
