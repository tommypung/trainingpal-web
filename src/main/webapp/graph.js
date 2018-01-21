function Collector(now)
{
	this.now = now;
	resetTime(this.now);
	this.buckets = {};

	function resetTime(d) {
		d.setHours(0);
		d.setMinutes(0);
		d.setSeconds(0);
		d.setMilliseconds(0);
	}

	function daysAgo(now, date) {
		var d = new Date(date.getTime());
		resetTime(d);
		return Math.floor((now.getTime() - d.getTime()) / (24 * 60 * 60 * 1000));
	}

	function spanCollect(name, low, high, date, value) {
		var distance = daysAgo(this.now, date);
		if (distance > high || distance < low) {
			return; // not within span
		}

		var o = this.buckets[name];
		if (!o)
			this.buckets[name] = {max: value.max, min: value.min};
		else {
			o.max = Math.max(o.max, value.max);
			o.min = Math.min(o.min, value.min);
		}
	}

	this.collect = function(date, value) {
		spanCollect.apply(this, ['all', 7, 30000, date, value]);
		spanCollect.apply(this, ['twoWeeks', 7, 21, date, value]);
		spanCollect.apply(this, ['today', 0, 0, date, value]);
		spanCollect.apply(this, ['4+', 29, 30000, date, value]);
		spanCollect.apply(this, ['3-4', 21, 28, date, value]);
		spanCollect.apply(this, ['2-3', 14, 20, date, value]);
		spanCollect.apply(this, ['1-2', 7, 13, date, value]);
		spanCollect.apply(this, ['0-1', 1, 6, date, value]);
	}

	this.compare = function(name, value) {
		var o = this.buckets[name];
		var today = this.buckets['today'];
		console.log("buckets: ", name, o, 'today', today);
		if (!o || !today) {
			return { };
		}

		console.log("todayPercentage = " + ((value - today.min) / (today.max - today.min)), value, today.max, today.min);
		var todayPercentage = Math.max(0, Math.min((value - today.min) / (today.max - today.min), 1.0));

		var r = {
				max: o.max,
				min: o.min,
				extremeDistance: o.max - o.min,
				maxDiff: today.max - o.max,
				minDiff: today.min - o.min,
				todayPercentage: todayPercentage,
				todayMin: today.min,
				todayMax: today.max,
				todayExtremeDistance: today.max - today.min,
				percentageDiff: value - (todayPercentage * (o.max - o.min) + o.min),
		};
		r.minDiffAbs = Math.abs(r.minDiff);
		r.maxDiffAbs = Math.abs(r.maxDiff);
		r.percentageDiffAbs = Math.abs(r.percentageDiff);
		r.largestDiff = Math.max(r.extremeDistance, r.maxDiffAbs, r.minDiffAbs, r.percentageDiffAbs, 3);
		r.smallDiff = Math.max(r.maxDiffAbs, r.minDiffAbs, 2);
		return r;
	}

	this.getGlobalRange = function(buckets)
	{
		console.log("getGlobalRange: ", arguments);
		var initial = this.buckets[buckets[0]];
		var min = initial.min, max = initial.max;
		for(var i=0;i<buckets.length;i++) {
			var j = this.buckets[buckets[i]];
			if (!j) {
				console.error("Could not find bucket: " + buckets[i]);
				continue;
			}
			min = Math.min(min, j.min);
			max = Math.max(max, j.max);
		}
		return [min, max];
	}
	
	this.getBucket = function(name) {
		return this.buckets[name];
	}
}

angular.module('graph', []).directive('myGraph', function() {
	return {
		scope: {
			collector: '&collector',
			buckets: '&buckets'
		},
		templateUrl: "graph.html",
		link: function($scope, element, attrs) {
			$scope.buckets = $scope.buckets();
			console.log(arguments);
			$scope.collector = $scope.collector();
			console.log("collector = ", $scope.collector);
			console.log("collector = ", attrs.collector);
			var bucketNames = [];
			for(var i=0;i<$scope.buckets.length;i++)
				bucketNames.push($scope.buckets[i].name);
			var range = $scope.collector.getGlobalRange(bucketNames);
			$scope.globalMin = range[0];
			$scope.globalMax = range[1];
			$scope.barWidth = 300;
			$scope.kgToPixelFactor = 300 / ($scope.globalMax - $scope.globalMin);
		},
		controller: function($scope) {
			this.getBucket = function(name) {
				return $scope.collector.getBucket(name);
			}
			this.getGlobalRange = function() {
				return [ $scope.globalMin, $scope.globalMax ];
			}
			this.getKgToPixelFactor = function() {
				return $scope.kgToPixelFactor;
			}
			this.compareToPrevious = function(bucketName, bucket)
			{
				function cmp(_prev, _bucket) {
					return [ _bucket.min - _prev.min, _bucket.max - _prev.max ]; 
				}
				for(var i=0;i<$scope.buckets.length;i++) {
					if ($scope.buckets[i].name == bucketName) {
						if (i == 0)
							return [ -2, -2 ];
						return cmp($scope.collector.getBucket($scope.buckets[i - 1].name), bucket);
					}
				}
				return [ -1, -1 ];
			}
		}
	};
}).directive('myGraphBar', function() {
	return {
		require: '^myGraph',
		scope: {
			bucketName: '@bucketName',
			width: '@width',
		},
		templateUrl: "graph-bar.html",
		link: function($scope, element, attrs, myGraph) {
			var bucket = myGraph.getBucket($scope.bucketName);
			console.log("bucketName = " ,$scope.bucketName, bucket);
			console.log("width = ", $scope.width);
			console.log(arguments);
			var range = myGraph.getGlobalRange();
			$scope.globalMin = range[0];
			$scope.globalMax = range[1];
			$scope.min = bucket.min;
			$scope.max = bucket.max;
			$scope.kgToPixelFactor = myGraph.getKgToPixelFactor();
			console.log("equation: ", $scope.min, $scope.globalMin, $scope.kgToPixelFactor);
			var colorCmp = myGraph.compareToPrevious($scope.bucketName, bucket);
			$scope.lowerColor = (colorCmp[0] <= 0) ? 'blue' : 'red';
			$scope.higherColor = (colorCmp[1] <= 0) ? 'blue' : 'red';
			console.log("colorcmp:", colorCmp);
			$scope.start = ($scope.min - $scope.globalMin) * parseFloat($scope.kgToPixelFactor);
			$scope.length = ($scope.max - $scope.min) * parseFloat($scope.kgToPixelFactor);
		}
	};
});