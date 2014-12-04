//
// enMarkdown
//
// Angular module that registers a directive. Use as
//
//  <script src=/${spath}/enshowdown.js>
//
//
//  <div data-markdown>
//    # Hello There
//    This is [an example](http://example.com/ "I'm the title") of an inline link.
//  </div>
//
// TODO: make it use ng-model, this solution is rather static
    
(function(){
	"use strict";
	
	var MODULE = angular.module("enMarkdown",[])
	
	MODULE.service( 'en$markdown', function() {
		return new Markdown.Converter();
	});
	
	MODULE.directive('markdown', function (en$markdown) {
	    return {
	        restrict: 'A',
	        link: function (scope, element, attrs) {
	            var htmlText = en$markdown.makeHtml(element.text());
	            element.html(htmlText);
	        }
	    };
	});
	
})()
