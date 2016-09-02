angular.module('myApp', []) //main controller
.controller('myCtrl',['$scope','$http',
function ($scope, $http) {
	$scope.legends ={}
	var VIDEO_PATH = "data/ht_video_pot_test_set/";
	//FILL link here
	var GOOGLE_FORMS_URL="";	
	$scope.video1 = "";
	$scope.video2 = "";
	$scope.score = 0.0;
	$scope.videoId1 = 0;
	$scope.videoId2 = 0;
		
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
					
//					tarr.push((data[j]*100).toFixed(0) + "%");
					tarr.push(data[j]);
				}
				lines.push(tarr);
			}
		}
		
		$scope.data = lines;
	};
	
	$scope.showVideos = function(vid1, vid2, score){
		$scope.videoId1=vid1;
		$scope.videoId2=vid2;
		$scope.video1 = VIDEO_PATH + $scope.legends[vid1]
		$scope.video2 = VIDEO_PATH + $scope.legends[vid2]
		$scope.score = score;
		
		$scope.playVideo(document.getElementById("video1"));
		$scope.playVideo(document.getElementById("video2"));

	}
	
	$scope.playVideo = function(video) {
		video.addEventListener('loadeddata', function() {
			video.play()
		}, false);
		
	}
	
	$scope.recordFeedback = function(){
		$scope.feedback_response="Posting.."
		$.ajax({
            url: GOOGLE_FORMS_URL,
            data: {	"entry.1986871126" : $scope.video1.substr(VIDEO_PATH.length),
        			"entry.489660422" : $scope.video2.substr(VIDEO_PATH.length), 
        			"entry.1932134194" : $scope.score, 
        			"entry.19555886": $scope.comments},
            type: "POST",
            dataType: "xml",
            statusCode: {
					0: function (){
						$scope.feedback_response="Error! Contact support"
					},
					200: function (){
						$scope.feedback_response="Posted. Thanks!"
 					},
 					404: function() {
 		                console.log("-1-1-1-1 WE GOT 404!");
 		            }
				}
        });
	}
	$scope.readCSV();
}])//Filter for percentage in css
.filter('percentage', ['$filter', function ($filter) {
	  return function (input, decimals) {
		  //works only for fractions
		  if(input>1){
			  return 0;
		  }
		    return $filter('number')(input * 100, decimals) + '%';
		  };
}])//Filter for range
.filter('range', function() {
	return function(input, min, max) {
		//works only for fractions
		if (input > 1) {
			return input;
		}
		if (input >= min && input <= max) {
			return input
		}
		return "";
	};
});

