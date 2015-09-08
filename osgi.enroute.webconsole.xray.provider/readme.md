# aQute X-Ray Web Console Plugin
A plugin for the [Apache Felix Webconsole][1]. When installed, this bundle 
will add a tab on the Web Console that provides a graphic overview of the 
OSGi framework, including services, bundles, and components. Colors are used to 
encode the different states. Bundles are shown to be started, stopped, starting, 
resolved, and installed. Services are shown to be used, looked for, or registered in the 
air. Components can be seen to be operating or not satisfied. Most objects have a tooltip 
with extra information and all objects can be clicked upon. Hovering over a bundle or 
service shows only the connections to/from the component. The display updates every 5 seconds 
to provide a continuous view on the operations of the framework it runs on.

## Compatibility
This plugin should run on WebConsole 3 and 4

[1]: http://felix.apache.org/site/apache-felix-web-console.html

_${Bundle-Version}_
