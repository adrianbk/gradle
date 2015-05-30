#!/usr/bin/env bash
CMD="TERM=xterm ./gradlew ${BUILD} -S --continue --console plain"
#if [ "${BUILD}" == "prIntegTestBuild7" ]; then
#    CMD="${CMD} -i"
#fi

echo "-------Executing command ------------"
echo "${CMD}"
echo "-------------------------------------"
eval ${CMD}

