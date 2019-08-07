# takes parameters: device, buildtype, statix_build_type, and repopick
node {
    currentBuild.displayName = "$DEVICE"
    currentBuild.description = "Build ID: $BUILD_NUMBER"
    def BUILD_TREE="$BUILD_HOME/stx-caf"
    stage('Sync') {
        telegramSend("Syncing source...")
    	sh '''#!/bin/bash
		cd '''+BUILD_TREE+'''
		. venv/bin/activate
		repo init --depth=1 -u https://github.com/StatiXOS/android_manifest.git -b 9-caf --no-tags
		rm -rf .repo/local_manifests
	    repo sync -d -c --force-sync --no-tags --no-clone-bundle -j8
	    repo forall -vc "git reset --hard"
		'''
    }
    stage('Clean') {
		sh '''#!/bin/bash
		cd '''+BUILD_TREE+'''
		make clean
		make clobber
		'''
  }
  stage('Build') {
      telegramSend("Starting build of $DEVICE")
      telegramSend("Job url: $BUILD_URL")
		sh '''#!/bin/bash +e
		cd '''+BUILD_TREE+'''
		. venv/bin/activate
		. build/envsetup.sh
		ccache -M 75G
		export USE_CCACHE=1
		export CCACHE_COMPRESS=1
		lunch statix_$DEVICE-$BUILDTYPE
		set -e
		if [[ ! -z "${REPOPICK}" ]]; then repopick -f ${REPOPICK}; else echo "No Commits to pick!"; fi
		mka bacon
		'''
  }
  stage('Upload') {
      telegramSend("Uploading build of $DEVICE")
      	sh '''#!/bin/bash
      	set -e
		echo "Deploying artifacts..."
		rsync --progress -a --include "statix_$DEVICE-*-$STATIX_BUILD_TYPE.zip" --exclude "*" $OUT_DIR_COMMON_BASE/stx-caf/target/product/$DEVICE/ deletthiskthx@frs.sourceforge.net:/home/pfs/project/statixos/$DEVICE/nuclear/
		'''
        telegramSend("Upload complete!")
  }
}
