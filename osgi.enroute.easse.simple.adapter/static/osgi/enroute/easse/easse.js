(function(){
	"use strict";
	
	var MODULE = angular.module("enEasse", []);

	

	//
	// Provider
	//
	
	function EASSEProvider() {
		
		if (!angular.isDefined(window.EventSource))
			throw "Requires EventSource";
		
		

		this.$get = function() {
			var nonce;
			
			function handle(topic, msgcb, errorcb, log,scope) {
				var instance 		= Math.floor(Math.random() * 1000000000000);
				var sseurl 			= "/sse/1/"+topic + "?instance="+instance;
				var sse;
				var sseOpen = 0;
				
				log = log || function(m) {console.log(m);};
				errorcb = errorcb || log;
				
			
				function init() {
					
					var url = sseurl;
					if (nonce) {
						url += "&_credentials=" + nonce;
					}
					
					sse = new EventSource(url);
					
					
					if ( scope )
						scope.$on("$destroy", function(){
							sse.close();
						});
					
					sse.onerror = function(event) {
						errorcb("sse error " + sseurl);
						sseOpen++;
					};
						
					sse.onopen = function(event) {
						log("sse open " + sseurl);
						sseOpen = 0;
					}

					sse.onmessage = function(event) {
						sseOpen = 0;
						var msg = JSON.parse(event.data);
						log("sse msg " + event.data);
						msgcb(msg);
					}
					
					//
					// Desperate attempt to reset the SSE
					//
					window.onbeforeunload = function() {
						log("abort sse " + sseurl);
						var xhr = new XMLHttpRequest();
						xhr.open("GET", '/sse/1?abort='+instance,false);
						xhr.withCredentials=true;
						xhr.send(null);
						
						//
						// Let's timeout so we do not hang the page
						//
						
						setTimeout(function() {
							xhr.abort();
						}, 2000); 
					}
				}
					
				window.setInterval( function() {
					if ( sseOpen > 3) {
						console.log("sse reset");
						sseOpen = 0;
						if ( sse )
							sse.close();
						
						init();
					} else if ( sseOpen > 0 || sse.readyState == 2)
						sseOpen++;
				}, 5000);
				
				init();
				
				return {
					close: function() {
						sse.close();
					}
				};
			}
			
			return {
				handle: handle,
				setNonce: function(n) {
					nonce=n;
				}
			};
		}
	}

	
	
	
	
	
	
	
	MODULE.provider("en$easse", EASSEProvider );
	
})();