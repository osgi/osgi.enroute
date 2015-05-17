


'use strict';

(function() {

	var MODULE = angular.module('osgi.enroute.iot.circuit', [ 'ngRoute', 'enJsonrpc','enEasse' ]);

	var alerts = [];
	var resolveBefore;
	
	function error(msg) {
		alerts.push({
			msg : msg,
			type : 'danger'
		});
	}

	function CircuitManager(circuitApp, $scope) {
		var THIS = this;

		THIS.refresh = function() {
			circuitApp.getWires().then( function(d) {
				THIS.wires = d;
			});
			circuitApp.getDevices().then(function(d) {
				THIS.ics = d;
			});
		}

		THIS.update = function(x) {
			THIS.refresh();
		}
		
		THIS.disconnect = function(id) {
			circuitApp.disconnect(id);
		}
		
		THIS.connect = function(fromDevice, fromPin, toDevice, toPin) {
			circuitApp.connect(fromDevice, fromPin, toDevice, toPin);
		}
		
		setInterval(function() {
			circuitApp.getDevices().then(function(d) {
				THIS.ics = d;
			});
		}, 500);
		
	}

	MODULE.config(function($routeProvider, en$jsonrpcProvider) {
		
		en$jsonrpcProvider.setNotification({
			error : error
		})

		resolveBefore = {
			circuitApp : en$jsonrpcProvider
					.endpoint("osgi.enroute.iot.circuit")
		};

		$routeProvider.when('/', {
			controller : mainProvider,
			templateUrl : '/osgi.enroute.iot.circuit/main/htm/home.htm',
			resolve : resolveBefore
		});
		$routeProvider.when('/about', {
			templateUrl : '/osgi.enroute.iot.circuit/main/htm/about.htm',
			resolve : resolveBefore
		});
		$routeProvider.otherwise('/');
	});

	MODULE.run(function($rootScope, $location, en$easse, en$jsonrpc) {
		$rootScope.page = function() {
			return $location.path();
		}
		resolveBefore.circuitApp().then(function(circuitApp) {
			$rootScope.circuit = new CircuitManager(circuitApp, $rootScope);
			en$easse.handle("osgi/enroute/iot/*",
						$rootScope.circuit.update, error);
		});

	});

	
	MODULE.directive("droplink", function() {
		return {
			restrict: 'A',
			scope: {
				device: "=device",
				pin: "=pin",
				droplink: "&droplink"
			},
	        link: function(scope, el, attrs, controller) {
	            el.bind("dragover", function(e) {
	                  if (e.preventDefault) {
	                    e.preventDefault(); // Necessary. Allows us to drop.
	                  }
	                  
	                  e.dataTransfer.dropEffect = 'move';  // See the section on the DataTransfer object.
	                  return false;
	            });
	            
	            el.bind("dragenter", function(e) {
	            	el.addClass("drag-ok");
	            });
	            el.bind("dragleave", function(e) {
	            	el.removeClass("drag-ok");
	            });

                el.bind("drop", function(e) {
                  if (e.preventDefault) {
                    e.preventDefault(); // Necessary. Allows us to drop.
                  }

                  if (e.stopPropogation) {
                    e.stopPropogation(); // Necessary. Allows us to drop.
                  }
                  var data = e.dataTransfer.getData("text").split("/");
                  
                  console.log( "connect " + scope.device + " " + scope.pin + " -> " + data[0] +" " + data[1]);
                  
                  scope.droplink({ fromDevice: data[0], fromPin: data[1], toDevice: scope.device, toPin: scope.pin});
                });
	        }
		}
	});

	MODULE.directive("draglink", function() {
		return {
			restrict: 'A',
			scope: {
				device: "=device",
				pin: "=pin"
			},
	        link: function(scope, el, attrs, controller) {
	            angular.element(el).attr("draggable", "true");

	            el.bind("dragstart", function(e) {
	                e.dataTransfer.setData('text', scope.device + "/" + scope.pin);
	            });

	            el.bind("dragend", function(e) {
	            });
	        }
		}
	});

	var mainProvider = function($scope) {
		$scope.circuit.refresh();
	}

})();
