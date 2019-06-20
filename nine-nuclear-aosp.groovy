# takes string parameters: device, buildtype, statix_build_type, and repopick
def BUILD_TREE = "/home/buildbot/stx-aosp"
node {
      stage('Sync') {
        telegramSend 'Starting build of $DEVICE $BUILDTYPE'
        telegramSend 'Job link: $BUILD_URL';
	    sh '''#!/bin/bash
		cd '''+BUILD_TREE+'''
		. venv/bin/activate
		repo init --depth=1 -u https://github.com/StatiXOS/android_manifest.git -b 9 --no-tags
		rm -rf .repo/local_manifests
	    repo sync -d -c --force-sync --no-tags --no-clone-bundle -j8
		'''
	telegramSend 'Sync completed.';
  }
  stage('Clean') {
		sh '''#!/bin/bash
		cd '''+BUILD_TREE+'''
		make clean
		make clobber
		'''
  }
  stage('Build') {
      telegramSend 'Starting build of $DEVICE';
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
		telegramSend 'Build completed, uploading.';
  }
  stage('Upload') {
      	sh '''#!/bin/bash
      	set -e
		echo "Deploying artifacts..."
		rsync --progress -a --include "statix_$DEVICE-*-$STATIX_BUILD_TYPE.zip" --exclude "*" $OUT_DIR_COMMON_BASE/stx-aosp/target/product/$DEVICE/ anayw2001@storage.osdn.net:/storage/groups/s/st/statixos/$DEVICE/9/
		'''
		telegramSend 'Build uploaded!';
		sh '''#!/bin/bash
		rm -rf out
		'''
  }
}
