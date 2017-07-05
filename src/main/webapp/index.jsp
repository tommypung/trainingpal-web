<!DOCTYPE html>
<html>
<head>
	<title>Trainingpal</title>
	<script src="https://www.gstatic.com/firebasejs/4.1.3/firebase.js"></script>
	<script>
	  // Initialize Firebase
	  var config = {
	    apiKey: "AIzaSyC7_d4VDAAVHlLsJv2ZXfunaEatQoemFT0",
	    authDomain: "trainingpal-web.firebaseapp.com",
	    databaseURL: "https://trainingpal-web.firebaseio.com",
	    projectId: "trainingpal-web",
	    storageBucket: "trainingpal-web.appspot.com",
	    messagingSenderId: "733491293394"
	  };
	  firebase.initializeApp(config);
	</script>
	<script type="text/javascript" src="angular.js"></script>
	<script src="Chart.bundle.js"></script>
	<script src="angular-chart.js"></script>
	<script type="text/javascript" src="script.js?dsf"></script>
</head>
<body ng-app="Trainingpal">
	<div ng-controller="MainController">
		<div ng-hide="user" class="weigh">
			<h1>Var god ställ dig på vågen</h1>
	   		<h1>{{lastWeight.kg}} KG</h1>
		</div>
		<div ng-show="user" class="view">
	   		<h1>Välkommen {{user.name}}</h1>
		</div>
	<h2>Din senaste vägning: {{lastWeight.kg}}</h2>
	<div class="comparedToLastDay">
		<h3>Jämfört med tidigare dag</h3>
		<span class="title">Max:</span> <span class="value">{{diff.lastDay.max}}</span>
		<span class="title">Avg:</span> <span class="value">{{diff.lastDay.avg}}</span>
		<span class="title">Min:</span> <span class="value">{{diff.lastDay.min}}</span>
	</div>

	<div class="todaysScore">
		<h3>Dagens resultat</h3>
		<span class="title">Max:</span> <span class="value">{{today.max | number:2:2}}kg</span><br/>
		<span class="title">Avg:</span> <span class="value">{{today.avg | number:2:2}}kg</span><br/>
		<span class="title">Min:</span> <span class="value">{{today.min | number:2:2}}kg</span><br/>
	</div>

		<pre style="font-size:0.2em">
{{lastWeight | json}}
		</pre>
		<div ng-if="user" style="width: 800px; height: 200px">
			<h1>Alla vikter</h1>
			<canvas width="800" height="200" id="line" class="chart chart-line" chart-data="data" chart-options="options" chart-click="onClick">
		</canvas>
		</div>
	</div>
</body>
</html>
