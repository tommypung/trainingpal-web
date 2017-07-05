<!DOCTYPE html>
<html>
<head>
	<base href="/">
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
	<script type="text/javascript" src="angular-route.js"></script>
	<script src="Chart.bundle.js"></script>
	<script src="angular-chart.js"></script>
	<script type="text/javascript" src="script.js?dsf"></script>
</head>
<body ng-app="Trainingpal">
	<div ng-controller="MainController">
		<div ng-view></div>
	</div>
</body>
</html>
