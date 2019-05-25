# takes parameters: device, buildtype, statix_build_type, and repopick
def BUILD_TREE = "/home/buildbot/stx-caf"
node {
      telegramSend 'Job link: $BUILD_URL';
      stage('Sync') {
    	sh '''#!/bin/bash
		cd '''+BUILD_TREE+'''
		. venv/bin/activate
		repo init --depth=1 -u https://github.com/StatiXOS/android_manifest.git -b 9-caf --no-tags
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
		telegramSend 'Build completed,  uploading.';
  }
  stage('Upload') {
      	sh '''#!/bin/bash
      	set -e
		cd '''+BUILD_TREE+'''
		echo "Deploying artifacts..."
		~/gdrive upload '''+BUILD_TREE+'''/out/target/product/*/statix_$DEVICE-*-$STATIX_BUILD_TYPE.zip
		rm -rf out
		'''
  }
}