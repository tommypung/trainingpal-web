var app = angular.module('Trainingpal', ["chart.js", 'ngRoute']);

app.config(['$routeProvider', '$locationProvider',
	function($routeProvider, $locationProvider) {
	$routeProvider
	.when('/newWeight/:user', {
		templateUrl: 'newWeight.html'
	})
	.when('/newWeight', {
		templateUrl: 'newWeight.html'
	})
	.when('/switchUser', {
		templateUrl: 'switchUser.html', controller: 'SwitchUserController',
	})
	.when('/moveWeight', {
		templateUrl: 'moveWeight.html', controller: 'MoveWeightController'
	})
	.when('/selectUserForNewWeight', {
		templateUrl: 'switchUser.html', controller: 'SelectUserForNewWeightController'
	})
	.when('/newUser', {
		templateUrl: 'newUser.html', controller: 'NewUserController'
	})
	.otherwise({
		templateUrl: 'newWeight.html' 
	});

	$locationProvider.html5Mode(false);
}]);

app.service('UserService', ['$rootScope', '$location', function($rootScope, $location) {
	function UserService() {
		this.user = null;
		var outer = this;
		this.users = null;

		this.selectUser = function(users) {
			this.users = users;
			$location.path("/selectUserForNewWeight");
		}

		this.setUser = function(user, force) {
			if (!force && this.user && this.user.id == user.id)
				return;

			this.user = user;
			$rootScope.$broadcast("UserChanged", this.user);
		}

		this.getSelectUsers = function() {
			return this.users;
		}

		this.getUser = function() {
			return this.user;
		}
	};

	return new UserService();
}]);

app.controller('SelectUserForNewWeightController', function($scope, $http, $filter, $route, $location, $routeParams, UserService) {
	var homeId = "Tommy";
	$scope.showLoadAll = true;
	$scope.showWeight = true;
	$scope.users = UserService.getSelectUsers();
	$scope.weight = $scope.lastWeight.kg;
	$scope.date = new Date($scope.lastWeight.date).getTime();

	$scope.setUser = function(user) {
		console.log("setuser says, $scope.lastWeight = " , $scope.lastWeight, $scope.$parent.lastWeight);
		var id = (user) ? user.id : -1;
		$http({method: "POST", url: "/weight/" + homeId + "/register/" + $scope.date + "/" + $scope.weight + "/" + id}).then(function(json) {
			if (json.data.status != 'ok')
				alert(json.data.error);
		});
	}
	$scope.loadAll = function() {
		$http({method: "GET", url: "/getUsers/" + homeId}).then(function(json) {
			$scope.users = json.data.users;
		});
		$scope.showAddUser = true;
	}
});

app.controller('SwitchUserController', function($scope, $http, $filter, $route, $location, $routeParams, UserService) {
	var homeId = "Tommy";
	$scope.users = [];
	$http({method: "GET", url: "/getUsers/" + homeId}).then(function(json) {
		$scope.users = json.data.users;
	});

	$scope.setUser = function(user) {
		UserService.setUser(user);
	}
});

app.controller('MoveWeightController', function($scope, $http, $filter, $route, $location, $routeParams, UserService) {
	var homeId = "Tommy";
	var user = UserService.getUser();
	$scope.users = [];
	$scope.weights = [];
	$http({method: "GET", url: "/getUsers/" + homeId}).then(function(json) {
		$scope.users = json.data.users;
	});

	$http({method: "GET", url: "/getWeight/" + user.id + "/1/months"}).then(function(json) {
		$scope.weights = [];
		for(var i=0;i<json.data.dates.length;i++) {
			$scope.weights.push( { date: json.data.dates[i], weight: json.data.weights[i] } );
		}
	});

	$scope.selectWeight = function(weight) {
		$scope.selected = weight;
	}

	$scope.selectUser = function(selectedUser) {
		var url = "/moveWeight/" + homeId + "/" + user.id + "/" + $scope.selected.date + "/" + $scope.selected.weight + "/" + ((selectedUser) ? selectedUser.id : -1);
		$http({method: "POST", url: url}).then(function(json) {
			if (json.data.status != 'ok')
				alert(json.data.error);
		});
	}
});

app.controller('MenuController', function($scope, $http, $filter, $route, $location, $routeParams) {
	$scope.switchUser = function() {
		$location.path("/switchUser");
	}
	$scope.moveWeight = function() {
		$location.path("/moveWeight");
	}
	$scope.startPage = function() {
		$location.path("/");
	}
});

app.controller('NewUserController', function($scope, $http, $filter, $route, $location, $routeParams, UserService, $rootScope) {
	var user = UserService.getUser();
	$scope.newInfo = {
			image: user.image,
			name: user.name,
			id: user.id,
			length: user.length
	};

	$scope.saveNewInfo = function() {
		$http({method: "POST", url: "/updateUser/info/" + user.id, data: $scope.newInfo}).then(function(json) {
			console.log("Got response from server", json);
		});
	}
});

app.controller('MainController', function($scope, $http, $filter, $route, $location, $routeParams, $rootScope, UserService) {
	var homeId = "Tommy";
	var db = firebase.database();

	console.log("routeParams: ", $routeParams);
	var overrideUserOneTime = $routeParams.user;

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
			data: []
	};

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

	$rootScope.$on('UserChanged', function(evt, user) {
		if (user.newAutoCreate)
			$location.path("/newUser");
		else
			$location.path("/newWeight");

		if (overrideUserOneTime) {
			console.log("Overrideuser onetime", overrideUserOneTime);
			user = overrideUserOneTime;
			overrideUserOneTime = null;
		}
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
					data: []
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

					$scope.today.data.push({x: date, y: weight});
				}
				compareOrIncreaseGroupedStats(monthly, $filter('date')(date, "yyyy-MM"), json.data.weights[i], date);
				compareOrIncreaseGroupedStats(weekly,  $filter('date')(date, "yyyy-WW"), json.data.weights[i], date);
				compareOrIncreaseGroupedStats(daily,   $filter('date')(date, "yyyy-MM-dd"), json.data.weights[i], date);
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
	});

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
		console.log("Incoming new lastWeight: ", $scope.lastWeight);

		convertMapToArray($scope.lastWeight, 'possibleUsers');

		if ($scope.lastWeight.possibleUsers.length == 1) {
			console.log("Loading latest user");
			$http({method: "GET", url: "/getUsers/byId/" + $scope.lastWeight.possibleUsers[0].id}).then(function(json) {
				console.log("got response", json);
				UserService.setUser(json.data.user, true);
			});
		} else {
			UserService.selectUser($scope.lastWeight.possibleUsers);
		}
	}

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

	$scope.todayOptions = {
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
