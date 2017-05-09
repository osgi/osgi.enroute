package osgi.enroute.jsonrpc.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A Web Resource that provides Jsonrpc javascript files for JSON RPC.
 * <p>
 * The JSON RPC bundle is explained at
 * <a href="http://enroute.osgi.org/services/osgi.enroute.jsonrpc.api.html">
 * service catalog JSON RPC entry</a>. The following is a skeleton of the
 * Javascript code:
 * 
 * <pre>
 * (function() {

	var MODULE = angular.module('osgi.enroute.examples.jsonrpc', [ 'ngRoute','enJsonrpc']);

	var resolveBefore = {};

	MODULE.config(function($routeProvider, en$jsonrpcProvider) {
		resolveBefore.exampleEndpoint = en$jsonrpcProvider.endpoint("exampleEndpoint");

		$routeProvider.when('/', {
			controller : MainController,
			templateUrl : '/osgi.enroute.examples.jsonrpc/main/htm/home.htm',
			resolve : resolveBefore
		});
		$routeProvider.otherwise('/');
		
	});

	MODULE.run(function($rootScope, en$jsonrpc) {
		resolveBefore.exampleEndpoint().then(function(exampleEndpoint) {
			$rootScope.exampleEndpoint = exampleEndpoint;
		});
	});

	var MainController = function($scope, en$jsonrpc) {
		$scope.upper = function(s) {
			$scope.exampleEndpoint.toUpper(s).then(function(d) {
				alerts.push({msg: d, type:"info"});
			});
		}
		$scope.welcome = $scope.exampleEndpoint.descriptor
	}

})();
 * </pre>
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS + "="
		+ JsonrpcConstants.JSONRPC_WEB_RESOURCE_PATH + ")${frange;" + JsonrpcConstants.JSONRPC_WEB_RESOURCE_VERSION
		+ "})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireJsonrpcWebResource {

	/**
	 * Define the default resource to return
	 * 
	 * @return the list of resources to include
	 */
	String[]resource() default {
			"jsonrpc.js"
	};

	/**
	 * Define the priority of this web resources. The higher the priority, the
	 * earlier it is loaded when all web resources are combined.
	 * 
	 * @return the priority
	 */
	int priority() default 100;
}
