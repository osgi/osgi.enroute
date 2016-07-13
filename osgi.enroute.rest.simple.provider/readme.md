# OSGI ENROUTE REST SIMPLE PROVIDER

${Bundle-Description}

## Example

## Configuration

Pid:`osgi.enroute.rest.simple`
	
| Field | Type | Description |
|----------|:-------------:|:-------------:|
|corsEnabled | Boolean | Whether to enable or disable CORS headers, default is '__false__' |
| allowOrigin	| String | The allowed origin hosts header '__Access-Control-Allow-Origin__', default '__*__' |
| allowMethods | String | Allowed HTTP client methods header '__Access-Control-Allow-Methods__' as comma seperated values, default '__GET, POST, PUT__' |
| allowHeaders | String | Allowed HTTP headers '__Access-Control-Allow-Headers__' as comma separated values, default '__Content-Type__'|
| maxAge | int | The Max Age for Access-Control* header '__Access-Control-Max-Age__' in seconds, default '__86400__' (24 hrs) |
| allowedMethods	| String | The methods that are allowed from client, Header '__Allow__', default '__GET, HEAD, POST, TRACE, OPTIONS__' |
	
e.g. to configure a REST service with CORS headers put the following in the configuration/configuration.json
```
{
	"service.pid": "osgi.enroute.rest.simple",
	"corsEnabled": true,
	"allowOrigin": "*",
	"allowMethods": "GET, POST, PUT, DELETE",
	"allowHeaders": "Content-Type",
	"maxAge": 86400,
	"allowedMethods": "GET, HEAD, POST, TRACE, OPTIONS"
}
```
## References

