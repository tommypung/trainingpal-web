<!DOCTYPE html>
<html>
<head>
<meta name="viewport"
	content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimal-scale=1.0, minimal-ui" />
<base href="/">
<title>Trainingpal</title>
<script src="https://www.gstatic.com/firebasejs/4.1.3/firebase.js"></script>
<script>
	// Initialize Firebase
	var config = {
		apiKey : "AIzaSyC7_d4VDAAVHlLsJv2ZXfunaEatQoemFT0",
		authDomain : "trainingpal-web.firebaseapp.com",
		databaseURL : "https://trainingpal-web.firebaseio.com",
		projectId : "trainingpal-web",
		storageBucket : "trainingpal-web.appspot.com",
		messagingSenderId : "733491293394"
	};
	firebase.initializeApp(config);
</script>
<script type="text/javascript" src="angular.js"></script>
<script type="text/javascript" src="angular-route.js"></script>
<script type="text/javascript" src="graph.js"></script>
<script src="Chart.bundle.js"></script>
<script src="angular-chart.js"></script>
<script type="text/javascript" src="script.js?dsf"></script>
<link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/bulma/0.6.2/css/bulma.min.css">
<link rel="stylesheet" type="text/css" href="style.css">
</head>
<body ng-app="Trainingpal">
	<div ng-controller="MainController">
		<div class="menu" ng-controller="MenuController">
			<div class="button" ng-click="openMenu = !openMenu">=</div>
			<div ng-show="openMenu" class="content">
				<div class="startPage" ng-click="startPage(); openMenu=!openMenu">Start</div>
				<div class="switchUser" ng-click="switchUser(); openMenu=!openMenu">Byt
					användare</div>
				<div class="moveWeight" ng-click="moveWeight(); openMenu=!openMenu">Flytta
					viktdata</div>
			</div>
		</div>
		<div ng-view></div>
	</div>
</body>
</html>
