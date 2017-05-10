/*
 * 
 */
'use strict';

/**
 * JSON RPC
 * 
 * This module handles JSON RPC requests.
 */
(function() {

	var MODULE = window.angular.module('enJsonrpc', []);

	/**
	 * Register a provider for the json rpc function. This provider can be
	 * initialized with:
	 * 
	 * url -> the url, default = /jsonrpc/2.0 notification -> must have error,
	 * warning, trace methods
	 * 
	 * TODO we should try to register the route when the provider is used.
	 */
	function JsonRPCProvider($routeProvider) {
		var url = '/jsonrpc/2.0/', notification;
		var instance;
		
		this.url = function(url_) {
			url = url_;
		};
		
		this.setNotification = function(n) {
			notification = n;
		};
		
		this.route = function(place) {
			$routeProvider.when( place || '/enroute/jsonrpc', {
				templateUrl : '/osgi/enroute/jsonrpc/htm/view.htm',
				controller : function($scope, en$jsonrpc) {
					$scope.tabs = [
		               {name: 'History', template:'/osgi/enroute/jsonrpc/htm/history.htm'},
		               {name: 'Endpoints', template:'/osgi/enroute/jsonrpc/htm/endpoints.htm'}
					];
					$scope.en$jsonrpc = en$jsonrpc;
					$scope.ping = function() { $scope.result = en$jsonrpc.ping(); };
				}
			});
		}

		this.endpoint = function(name,target) {
			return function() {
				return instance.endpoint(name,target);
			}
		}

		
		this.$get = function($http, $q) {

			function Rpc() {
				this.endpoints = [];
				this.history = [];
				this.roundtrip = {
					avg : 0,
					max : 0,
					min : 100000000
				};
				this.nextid = 1000;
				this.url = url;
				this.notification = notification;
			}

			Rpc.prototype.send = function(ep, method, args) {
				// trace && notification && notification.trace("send " +
				// this.url + " " + method + " " args);

				var promise=$q.defer();
				var THIS = this;
				var xargs = Array.prototype.slice.call(args);
				var msg = {
					jsonrpc : '2.0',
					method : method,
					params : xargs,
					id : this.nextid++
				};
				var start = new Date().getTime();

				this.history.push(msg);
				if (this.history.length > 200)
					this.history.splice(0, 100);

				$http
						.put(url + ep, msg)
						.success(
								function(data, status, headers) {

									var roundtrip = new Date().getTime()
											- start;
									msg.roundtrip = roundtrip;
									if  (THIS.roundtrip.avg == 0)
										THIS.roundtrip.avg = roundtrip;
									
									THIS.roundtrip.avg += 0.1 * (roundtrip - THIS.roundtrip.avg);
									THIS.roundtrip.max = Math
											.max(THIS.roundtrip.max, roundtrip);
									THIS.roundtrip.min = Math
											.min(THIS.roundtrip.min, roundtrip);

									msg.result = data;
									if (data.error) {
										var error = "[" + data.error.code
												+ "] " + data.error.message;
										if ( THIS.notification )
											THIS.notification.error(error);
										console.log("Error in JSONRPC " + error)
										promise.reject(error);
									} else
										promise.resolve(data.result)
								}).error(function(data, status, config) {
							var error = report(data,status, config);
							msg.error = error;
							if ( notification) 
								notification.error(msg.error);
							msg.data = data;
							promise.reject(error);
						});

				return promise.promise;
			}

			/*
			 * Retrieve an endpoint. If a second 'target' argument is given then
			 * this will also get all the remote methods added to it. 
			 */
			Rpc.prototype.endpoint = function(epName, target) {
				var THIS = this;
				
				function setMethod(target, endpoint, method, rpc) {
					var f = function() {
						return THIS.send(endpoint, method, arguments);
					};
					target[method] = f;
				}
				

				return this.send(epName, "__hi", []).then(function(ep) {
					if ( !angular.isDefined(ep) ) {
						console.log('no such endpoint ' + epName);
						return null;
					} else {
						THIS.endpoints.push(ep);
	
						for ( var i = 0; i < ep.methods.length; i++) {
							//
							// Add a method to the endpoint
							//
							setMethod(ep, epName, ep.methods[i]);
							if ( target )
								setMethod(target, epName, ep.methods[i]);
						}
						return ep;
					}
				});
			}; // endpoint

			Rpc.prototype.ping = function() {
				this.send("", "__ping", [ new Date().getTime() ]).then( function(d) {console.log(d);});
			};
			Rpc.prototype.getEndpoint = function(name) {
				for ( var i in this.endpoints) {
					var ep = this.endpoints[i];
					if ( ep.name == name)
						return ep;
				}
				return undefined;
			};
			Rpc.prototype.clear = function() {
				this.history = [];
			};

			return instance=new Rpc();
		};// $get

		return this;
	}
	MODULE.provider('en$jsonrpc', JsonRPCProvider);

	/**
	 * Report an http error
	 */
	function report(data, status, config) {
		switch (status) {
		case 403:
			return "[403] Forbidden ";
		case 404:
			return "[404] Not found ";
		case 405:
			return "[405] Verb not supported ";
		case 500:
			return "[500] Server error ";
		default:
			return "[" + status + "] Failed ";
		}
	}

}).call();
