# takes string parameters: device, buildtype, statix_build_type, and repopick
node {
    currentBuild.displayName = "$DEVICE"
    currentBuild.description = "Build ID: $BUILD_NUMBER"
    def BUILD_TREE = "$BUILD_HOME/stx-aosp"
      stage('Sync') {
          telegramSend("Syncing source...")
	    sh '''#!/bin/bash
		cd '''+BUILD_TREE+'''
		. venv/bin/activate
		repo init --depth=1 -u https://github.com/StatiXOS/android_manifest.git -b 9 --no-tags
		rm -rf .repo/local_manifests
	    repo sync -d -c --force-sync --no-tags --no-clone-bundle -j8
	    repo forall -vc "git reset --hard"
	    repo forall -vc "git checkout"
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
      telegramSend("Starting build for $DEVICE")
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
		telegramSend("Build complete!")
  }
  stage('Upload') {
        telegramSend("Uploading build of $DEVICE")
      	sh '''#!/bin/bash
      	set -e
		echo "Deploying artifacts..."
		rsync --progress -a --include "statix_$DEVICE-*-$STATIX_BUILD_TYPE.zip" --exclude "*" $OUT_DIR_COMMON_BASE/stx-aosp/target/product/$DEVICE/ deletthiskthx@frs.sourceforge.net:/home/pfs/project/statixos/$DEVICE/nuclear/
		'''
		telegramSend("Upload complete!")
  }
}
