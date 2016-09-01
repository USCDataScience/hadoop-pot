function myCtrl($scope, $http) {
	$scope.legends ={}
	var VIDEO_PATH = "data/ht_video_pot_test_set/"
	$scope.video1 = "";
	$scope.video2 = "";
	$scope.score = 0.0;
	
		
	$scope.readCSV = function() {
		// http get request to read CSV file content
		$http.get('data/formatted_similarity_calc.csv').success($scope.processData);
	};

	$scope.processData = function(allText) {
		// split content based on new line
		var allTextLines = allText.split(/\r\n|\n/);
		var headers = allTextLines[0].split(',');
		var lines = [];

		for ( var i = 0; i < allTextLines.length; i++) {
			// split content based on comma
			var data = allTextLines[i].split(',');
			if (data.length == headers.length) {
				var tarr = [];
				for ( var j = 0; j < headers.length; j++) {
					if(i==0){
						tarr.push(j);
						$scope.legends[j]=data[j]
						continue;
					}
					if(j==0){
						tarr.push(i);
						continue;
					}
					
					tarr.push(data[j]);
				}
				lines.push(tarr);
			}
		}
		
		$scope.data = lines;
	};
	
	$scope.showVideos = function(vid1, vid2, score){
		$scope.video1 = VIDEO_PATH + $scope.legends[vid1]
		$scope.video2 = VIDEO_PATH + $scope.legends[vid2]
		$scope.score = score;
	}
	
	$scope.readCSV();
}