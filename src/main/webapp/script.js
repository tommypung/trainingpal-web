var app = angular.module('Trainingpal', ["chart.js", 'ngRoute']);

app.config(['$routeProvider', '$locationProvider',
  function($routeProvider, $locationProvider) {
    $routeProvider
      .when('/newWeight', {
        templateUrl: 'newWeight.html'
      })
      .when('/newUser', {
        templateUrl: 'newUser.html',
      })
      .otherwise({
    	 templateUrl: 'newWeight.html' 
      });

    $locationProvider.html5Mode(false);
}]);

app.controller('MainController', function($scope, $http, $filter, $route, $location) {
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
	$scope.newInfo = {
			
	};

	$scope.saveNewInfo = function() {
		$http({method: "POST", url: "/updateUser/info/" + $scope.user.id, data: $scope.newInfo}).then(function(json) {
			console.log("Got response from server", json);
		});
	}

	function compareOrIncreaseGroupedStats(obj, str, value, date)
	{
		if (obj.last != undefined && obj.last != str)
		{
			obj.last = undefined;
			obj.avg = obj.sum / obj.count;
			console.log("obj.avg(" + obj.avg + ") = obj.sum(" + obj.sum + ") / obj.count(" + obj.count + ") - max=" + obj.max + "-min=" + obj.min);
			obj.data[0].push({x: obj.lastDate, y: obj.avg});
			obj.data[1].push({x: obj.lastDate, y: obj.max});
			obj.data[2].push({x: obj.lastDate, y: obj.min});

			if (value == undefined || date == undefined)
				return;
		}

		if (obj.last == undefined)
		{
			obj.last = str;
			obj.max = value;
			obj.min = value;
			obj.sum = value;
			obj.count = 1;
			obj.lastDate = date;
		}
		else if (obj.last == str)
		{
			if (value > obj.max)
				obj.max = value;
			if (value < obj.min)
				obj.min = value;
			obj.sum += value;
			obj.count++;
			obj.lastDate = date;
		}
	
	}

	function setNewUser(user) {
		if (user.newAutoCreate)
			$location.path("/newUser");
		else
			$location.path("/newWeight");

		console.log("Setting new user", user);
		$scope.user = user;
		$scope.series[0] = user;
		$scope.data = { };

		$http({method: "GET", url: "/getWeight/" + user.id + "/3/months"}).then(function(json) {
			console.log("Got response from server", json);
			//$scope.data = json.data.weights;
			$scope.labels = [];
			$scope.data = {
					data: [],
					labels: [],
					datasets: [ {
						label: "Dataset with point data",
						backgroundColor: "rgba(255, 255, 0, 0.5)",
						borderColor: "#ff0000",
						fill: false,
						data: []
					}] };
			var today = new Date();
			$scope.today = {
					sum: 0,
					count: 0,
			};
			var data = [ [], [], [] ];
			var monthly = { data: [[], [], []], last: undefined, max: undefined, min: undefined, count: undefined, sum: undefined, lastDate: undefined };
			var weekly =  { data: [[], [], []], last: undefined, max: undefined, min: undefined, count: undefined, sum: undefined, lastDate: undefined };
			var daily =   { data: [[], [], []], last: undefined, max: undefined, min: undefined, count: undefined, sum: undefined, lastDate: undefined };

			for(var i=0;i<json.data.dates.length;i++) {
				var date = new Date(json.data.dates[i]);
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
				
				compareOrIncreaseGroupedStats(monthly, $filter('date')(date, "yyyy-MM"), json.data.weights[i], date);
				compareOrIncreaseGroupedStats(weekly,  $filter('date')(date, "yyyy-WW"), json.data.weights[i], date);
				compareOrIncreaseGroupedStats(daily,   $filter('date')(date, "yyyy-MM-dd"), json.data.weights[i], date);
/*
				data[0].push( { x: json.data.dates[i], y: json.data.weights[i] } );
				data[1].push( { x: json.data.dates[i], y: json.data.weights[i] + 10 } );
				data[2].push( { x: json.data.dates[i], y: json.data.weights[i] - 10} );
				*/
			}

			compareOrIncreaseGroupedStats(monthly, undefined, json.data.weights[i], undefined);
			compareOrIncreaseGroupedStats(weekly,  undefined, json.data.weights[i], undefined);
			compareOrIncreaseGroupedStats(daily,   undefined, json.data.weights[i], undefined);

			console.log("data2 = ", daily.data);
			$scope.data2 = daily.data;
			$scope.data = daily.data;
			$scope.today.avg = $scope.today.sum / $scope.today.count;

			$scope.data = {
					datasets: [{
						backgroundColor: "red",
						borderColor: "red",
						data: [ 10, 20, 30, 40, 50, 60, 70],
						label: 'D0',
						fill: '-1'
					},
					{
						backgroundColor: "green",
						borderColor: "green",
						data: [ 20, 30, 40, 50, 60, 70, 80],
						label: 'D1',
						fill: '-1'
					}
					]
			};

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

	$scope.options2 = {
			maintainAspectRatio: false,
			spanGaps: false,
			elements: {
				line: {
					tension: 0.000001
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
					],
					yAxes: [{
						stacked: false
					}]
			},
			plugins: {
				filler: {
					propagate: false
				}
			}
	};	

	$scope.datasets2 = [
		{
			backgroundColor: "rgba(120, 120, 255, 0.2)",
			borderColor: "rgba(0,0,0,0)",
			label: 'Avg',
			fill: false
		},
		{
			backgroundColor: "rgba(120, 120, 255, 0.2)",
			borderColor: "rgba(0,0,0,0)",
			pointBackgroundColor: "rgba(0,0,0,0)",
			pointBorderColor: "rgba(0,0,0,0)",
			label: 'Min',
			fill: '-1'
		},
		{
			backgroundColor: "rgba(120, 120, 255, 0.2)",
			borderColor: "rgba(0,0,0,0)",
			pointBackgroundColor: "rgba(0,0,0,0)",
			pointBorderColor: "rgba(0,0,0,0)",
			label: 'Max',
			fill: '-1'
		}
		];

	$scope.data2 = [ [[0,0]], [[0,0]], [[0,0]] ];
	$scope.data = [ [[0,0]], [[0,0]], [[0,0]] ];

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
