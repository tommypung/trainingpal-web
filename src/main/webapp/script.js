var app = angular.module('Trainingpal', ["chart.js"]);

app.controller('MainController', function($scope, $http, $filter) {
	var homeId = "Tommy";
	var db = firebase.database();

	$scope.lastWeight = undefined;
	$scope.user = undefined;
	$scope.diff = {
			lastDay : {
				max: "+0.2kg",
				avg: "-0.1kg",
				min: "+0.3kg"
			}
	};
	$scope.today = {
	};

	function setNewUser(user) {
		console.log("Setting new user", user);
		$scope.user = user;
		$scope.series[0] = user;
		$scope.data = [ 0, 0, 0, 0, 0 ];

		$http({method: "GET", url: "/getWeight/" + user.id + "/3/months"}).then(function(json) {
			console.log("Got response from server", json);
			//$scope.data = json.data.weights;
			$scope.labels = [];
			$scope.data = { data: [], labels: [], datasets: [ {
				label: "Dataset with point data",
				backgroundColor: "rgba(255, 255, 0, 0.5)",
				borderColor: "#ff0000",
				fill: false,
				data: []
			}] };
			var today = new Date();
			var data = [];
			$scope.today = {
					sum: 0,
					count: 0,
			};
			for(var i=0;i<json.data.dates.length;i++) {
				var date = new Date(json.data.dates[i])
				var parsedDate = $filter('date')(date, "yyyy-MM-dd hh:mm");
				if (date.getDate() == today.getDate() && date.getYear() == today.getYear() && date.getMonth() == today.getMonth())
				{
					var weight = json.data.weights[i];
					$scope.today.sum += weight;
					if (!$scope.today.max)
						$scope.today.max = weight;
					else
						$scope.today.max = Math.max(weight, $scope.today.max);
					if (!$scope.today.min)
						$scope.today.min = weight;
					else
						$scope.today.min = Math.min(weight, $scope.today.min);
					$scope.today.count++;
				}
				//$scope.labels.push(json.data.dates[i]);//$filter('date')(new Date(json.data.dates[i]), "yyyy-MM-dd hh:mm"));
				data.push( { x: json.data.dates[i], y: json.data.weights[i] } );
			}
			$scope.today.avg = $scope.today.sum / $scope.today.count;
			$scope.data = data;
		});
	}

	function convertMapToArray(object, key)
	{
		var arr = [];
		for(var k in object[key]) {
			arr.push(object[key][k]);
		}
		object[key] = arr;
	}

	function applyNewWeight(lastWeight) {
		$scope.lastWeight = lastWeight;
		console.log("Incoming new lastWeight: " + $scope.lastWeight);

		convertMapToArray($scope.lastWeight, 'possibleUsers');

		if ($scope.lastWeight.possibleUsers.length == 1)
			setNewUser($scope.lastWeight.possibleUsers[0]);
	}

	console.log("firebase", firebase);
	var lastWeight= firebase.database().ref('homes/' + homeId + '/lastWeight');
	lastWeight.on('value', function(snapshot) {
		console.log("Snapshot coming in", snapshot);
		$scope.$apply((function(val) {applyNewWeight(val);})(snapshot.val()));
	});

	$scope.labels = [ ];
	$scope.series = [ ];
	$scope.data = [ ];
	$scope.onClick = function (points, evt) {
		console.log(points, evt);
	};
	var timeFormat = 'YYYY-MM-dd hh:mm';
	$scope.options = {
			elements: {
				line: {
					fill: false,
					borderColor: "rgba(255,0,0,0.5)",
					borderWidth: 3,
					cubicInterpolationMode: 'monotone',
					pointStyle: 'star',
					borderCapStyle: 'star'
				}
			},
			scales: {
				xAxes: [
					{
						type: 'time',
						display: true,
						time: {
							tooltipFormat: timeFormat,
							displayFormats: {
								millisecond: 'HH:mm:ss.SSS', // 11:20:01.123 AM,
								second: 'HH:mm:ss', // 11:20:01 AM
								minute: 'HH:mm', // 11:20:01 AM
								hour: 'MM-dd HH', // Sept 4, 5PM
								day: 'll', // Sep 4 2015
								week: 'YYYY-MM-dd', // Week 46, or maybe "[W]WW - YYYY" ?
								month: 'YYYY-MMM', // 2015-Sept
								quarter: '[Q]Q - YYYY', // Q3
								year: 'YYYY' // 2015
							}
						},
						scaleLabel: {
							display: true,
							labelString: 'Date'
						}
					}
					]
			}
	};
});
