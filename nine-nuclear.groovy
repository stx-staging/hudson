# depends on extra plugins: telegram-notifications

node('master') {
  // in this array we'll place the jobs that we wish to run
  def devices = [];
  stage('Preparation') { // for display purposes
    def url = "https://raw.githubusercontent.com/stx-staging/hudson/master/statix-jenkins-devices".toURL();
    println url;
    devices = url.readLines();
  }
  stage('Starting Builds') {
    telegramSend 'Starting builds'
    for (int i = 0; i < devices.size(); i++) {
      println devices[i];
      def device = devices[i].split();
      println device;
      if (device.size() == 4 && !device[0].startsWith( '#' )) {
        def codename = device[0];
        def buildtype = device[1];
        def statix_buildtype = device[2];
        def jobname = device[3];
        telegramSend 'started build job for "${codename}"-"${buildtype}"';
        build job: "${jobname}", parameters: [
          string(name:'DEVICE', value:"${codename}"),
          string(name:'BUILDTYPE', value: "${buildtype}"),
          string(name:'STATIX_BUILD_TYPE', value: "${statix_buildtype}"),
          string(name: 'REPOPICK', value: "1724 1725 1726 1727 1728")]
      }
    }
  }
}
